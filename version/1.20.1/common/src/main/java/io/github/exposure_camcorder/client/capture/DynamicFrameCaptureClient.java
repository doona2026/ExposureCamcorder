package io.github.exposure_camcorder.client.capture;

import com.mojang.logging.LogUtils;
import io.github.exposure_camcorder.ExposureCamcorder;
import io.github.exposure_camcorder.PlatformHelper;
import io.github.exposure_camcorder.compatibility.exposure.ExposureAccess;
import io.github.exposure_camcorder.network.packet.c2s.DynamicCaptureFrameDataC2SP;
import io.github.exposure_camcorder.network.packet.c2s.DynamicCaptureStopC2SP;
import io.github.exposure_camcorder.network.packet.s2c.DynamicCaptureStartS2CP;
import io.github.exposure_camcorder.network.packet.s2c.DynamicCaptureStateS2CP;
import io.github.exposure_camcorder.world.session.DynamicCaptureSessionEndReason;
import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.client.capture.Capture;
import io.github.mortuusars.exposure.client.capture.action.CaptureAction;
import io.github.mortuusars.exposure.client.capture.palettizer.Palettizer;
import io.github.mortuusars.exposure.client.capture.task.BackgroundScreenshotCaptureTask;
import io.github.mortuusars.exposure.client.capture.template.CaptureTemplate;
import io.github.mortuusars.exposure.client.image.Image;
import io.github.mortuusars.exposure.client.util.Minecrft;
import io.github.mortuusars.exposure.data.ColorPalette;
import io.github.exposure_camcorder.network.Packets;
import io.github.mortuusars.exposure.util.TranslatableError;
import io.github.mortuusars.exposure.util.cycles.task.EmptyTask;
import io.github.mortuusars.exposure.util.cycles.task.Result;
import io.github.mortuusars.exposure.util.cycles.task.Task;
import io.github.mortuusars.exposure.world.camera.capture.CaptureParameters;
import io.github.mortuusars.exposure.world.camera.capture.Projection;
import io.github.mortuusars.exposure.world.entity.CameraHolder;
import io.github.mortuusars.exposure.world.level.storage.ExposureData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class DynamicFrameCaptureClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_CONSECUTIVE_CAPTURE_FAILURES = 40;
    private static final CaptureTemplate CAPTURE_SUPPORT = new CaptureTemplate() {
        @Override
        public Task<?> createTask(CaptureParameters params) {
            return new EmptyTask<>();
        }
    };

    private static @Nullable String sessionId;
    private static @Nullable CaptureParameters captureParams;
    private static @Nullable DynamicRecordingClientState recordingState;
    private static int captureIntervalTicks;
    private static int maxFrames;
    private static int nextFrameIndex;
    private static boolean captureInFlight;
    private static boolean stopRequested;
    private static long lastCaptureGameTime = -1L;
    private static long captureEnqueuedGameTime = -1L;
    private static long lastStuckWarningGameTime = -1L;
    private static boolean keyUseWasDown;
    private static boolean freshPressPending;
    private static int consecutiveCaptureFailures;

    public static void init() {
    }

    public static void trackKeyState(Minecraft mc) {
        boolean keyUseDown = mc.options.keyUse.isDown();
        if (keyUseDown && !keyUseWasDown) {
            freshPressPending = true;
        }
        keyUseWasDown = keyUseDown;
    }

    public static boolean consumeFreshPress() {
        boolean pending = freshPressPending;
        freshPressPending = false;
        return pending;
    }

    public static void clearFreshPress() {
        freshPressPending = false;
    }

    public static void startSession(DynamicCaptureStartS2CP packet) {
        reset();
        sessionId = packet.sessionId();
        captureParams = packet.captureParameters();
        captureIntervalTicks = packet.captureIntervalTicks();
        maxFrames = packet.maxFrames();
        recordingState = new DynamicRecordingClientState(packet.sessionId(), 0, maxFrames, maxFrames);
        nextFrameIndex = 0;
        captureInFlight = false;
        stopRequested = false;
        lastCaptureGameTime = -1L;
    }

    public static void tick() {
        if (sessionId == null || recordingState == null || stopRequested) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        long currentGameTime = mc.level.getGameTime();

        if (recordingState.recordedFrames() >= maxFrames) {
            requestStop(DynamicCaptureSessionEndReason.FILM_EXHAUSTED);
            return;
        }

        if (captureInFlight) {
            if (captureEnqueuedGameTime >= 0L && currentGameTime - captureEnqueuedGameTime >= 100L
                    && currentGameTime - lastStuckWarningGameTime >= 100L) {
                lastStuckWarningGameTime = currentGameTime;
                LOGGER.warn("Dynamic capture task for session '{}' has been in flight for {} ticks without completing. "
                        + "Check whether the viewfinder is open or a capture task is failing.", sessionId,
                        currentGameTime - captureEnqueuedGameTime);
            }
            return;
        }

        if (lastCaptureGameTime >= 0L && currentGameTime - lastCaptureGameTime < captureIntervalTicks) {
            return;
        }
        lastCaptureGameTime = currentGameTime;
        captureInFlight = true;
        captureEnqueuedGameTime = currentGameTime;
        ExposureClient.cycles().enqueueTask(createCaptureTask(nextFrameIndex));
    }

    public static void updateState(DynamicCaptureStateS2CP packet) {
        if (packet.stopping() && sessionId != null && packet.sessionId().equals(sessionId)) {
            LOGGER.info("Server ended dynamic capture session '{}': {} frames recorded by server.", sessionId,
                    packet.recordedFrames());
            reset();
        }
    }

    public static boolean hasActiveSession() {
        return sessionId != null && !stopRequested;
    }

    public static void requestStop(DynamicCaptureSessionEndReason reason) {
        if (sessionId == null || stopRequested) {
            return;
        }

        stopRequested = true;
        LOGGER.info("Client stopping dynamic capture session '{}': reason={}, framesSent={}.", sessionId, reason,
                recordingState == null ? 0 : recordingState.recordedFrames());
        Packets.sendToServer(new DynamicCaptureStopC2SP(sessionId, reason));
    }

    public static void reset() {
        if (sessionId != null) {
            LOGGER.info("Dynamic capture session '{}' client state cleared (framesSent={}).", sessionId,
                    recordingState == null ? 0 : recordingState.recordedFrames());
        }
        sessionId = null;
        captureParams = null;
        recordingState = null;
        captureIntervalTicks = 0;
        maxFrames = 0;
        nextFrameIndex = 0;
        captureInFlight = false;
        stopRequested = false;
        lastCaptureGameTime = -1L;
        captureEnqueuedGameTime = -1L;
        lastStuckWarningGameTime = -1L;
        freshPressPending = false;
        consecutiveCaptureFailures = 0;
    }

    public static @Nullable DynamicRecordingClientState getRecordingState() {
        return recordingState;
    }

    private static Task<?> createCaptureTask(int frameIndex) {
        CaptureParameters params = captureParams;
        if (params == null || params.exposureId().isEmpty()) {
            LOGGER.error("Failed to capture frame {} of session '{}': capture parameters are missing.",
                    frameIndex, sessionId);
            captureInFlight = false;
            onFrameCaptureFailed();
            return new EmptyTask<>();
        }

        @Nullable Entity entity = Minecrft.level().getEntity(params.cameraHolderId().orElse(Minecrft.player().getId()));
        if (entity == null) {
            LOGGER.error("Failed to capture frame {} of session '{}': camera holder cannot be obtained.",
                    frameIndex, sessionId);
            captureInFlight = false;
            onFrameCaptureFailed();
            return new EmptyTask<>();
        }

        @Nullable CameraHolder holder = entity instanceof CameraHolder ? (CameraHolder) entity : null;

        Holder<ColorPalette> palette = CAPTURE_SUPPORT.getColorPalette(params);

        String sessionIdAtEnqueue = sessionId;

        Task<ExposureData> captureTask = Capture.of(createScreenshotTask(),
                        CaptureAction.setCameraEntity(entity),
                        CaptureAction.forceRegularOrSelfieCamera(holder),
                        CaptureAction.optional(params.fov(), CaptureAction::setFov),
                        CaptureAction.optional(!Config.Client.KEEP_POST_EFFECT.get(), CaptureAction::disablePostEffect),
                        CaptureAction.optional(params.filter(), filter -> CaptureAction.setFilter(java.util.Optional.of(filter))),
                        CaptureAction.modifyGamma(params.getShutterSpeed()),
                        CaptureAction.optional(params.getFlash(), () -> CaptureAction.flash(entity)))
                .handleErrorAndGetResult(CAPTURE_SUPPORT.printCasualErrorInChat())
                .thenAsync(CAPTURE_SUPPORT.applyEffectsToImage(params))
                .thenAsync(Palettizer.fromDitherMode(params.filmProperties().ditherMode()).palettizeAndClose(palette.value()))
                .then(CAPTURE_SUPPORT.convertToExposureData(palette, CAPTURE_SUPPORT.createExposureTag(params, false)));

        if (params.projection().isPresent()) {
            Projection projection = params.projection().get();

            captureTask = captureTask.overridenBy(Capture.of(Capture.path(projection.path()),
                            CaptureAction.optional(params.cameraId(), CaptureAction::interplanarProjection))
                    .logErrorAndGetResult(LOGGER)
                    .thenAsync(CAPTURE_SUPPORT.applyEffectsToImage(params.mutable().setCropFactor(1f).build()))
                    .thenAsync(Palettizer.fromDitherMode(projection.mode()).palettizeAndClose(palette.value()))
                    .then(CAPTURE_SUPPORT.convertToExposureData(palette,
                            CAPTURE_SUPPORT.createExposureTag(params, true))));
        }

        return captureTask.acceptAsync(exposureData -> onFrameCaptured(sessionIdAtEnqueue, frameIndex, exposureData))
                .onError(error -> onFrameCaptureFailed(frameIndex, error));
    }

    private static void onFrameCaptured(String sessionIdAtEnqueue, int frameIndex, ExposureData exposureData) {
        captureInFlight = false;
        consecutiveCaptureFailures = 0;
        if (sessionId == null || !sessionId.equals(sessionIdAtEnqueue)) {
            return;
        }

        String exposureId = ExposureAccess.createExposureId(sessionId, frameIndex);
        Packets.sendToServer(new DynamicCaptureFrameDataC2SP(sessionId, frameIndex, exposureId, exposureData));

        int recordedFrames = nextFrameIndex + 1;
        nextFrameIndex = recordedFrames;
        recordingState = new DynamicRecordingClientState(sessionId, recordedFrames, maxFrames,
                Math.max(0, maxFrames - recordedFrames));

        if (recordedFrames % 25 == 0 || recordedFrames == 1) {
            LOGGER.info("Dynamic capture session '{}': uploaded frame {}/{}.", sessionId, recordedFrames, maxFrames);
        }
    }

    private static void onFrameCaptureFailed(int frameIndex, TranslatableError error) {
        captureInFlight = false;
        LOGGER.warn("Failed to capture frame {} of session '{}': {} ({})", frameIndex, sessionId, error.key(), error.code());
        onFrameCaptureFailed();
    }

    private static void onFrameCaptureFailed() {
        consecutiveCaptureFailures++;
        if (consecutiveCaptureFailures >= MAX_CONSECUTIVE_CAPTURE_FAILURES) {
            LOGGER.warn("Dynamic capture session '{}' has failed {} consecutive captures; stopping the session.",
                    sessionId, consecutiveCaptureFailures);
            requestStop(DynamicCaptureSessionEndReason.INTERRUPTED);
        }
    }

    private static Task<Result<Image>> createScreenshotTask() {
        if (ExposureClient.shouldUseDirectCapture() || isIrisOrOculusLoaded()) {
            return new ViewfinderDirectScreenshotCaptureTask();
        }
        return new BackgroundScreenshotCaptureTask();
    }

    private static boolean isIrisOrOculusLoaded() {
        return PlatformHelper.isModLoaded("iris") || PlatformHelper.isModLoaded("oculus");
    }
}
