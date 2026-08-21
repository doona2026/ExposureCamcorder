package io.github.exposure_camcorder.client.capture;

import com.mojang.logging.LogUtils;
import io.github.exposure_camcorder.PlatformHelper;
import io.github.exposure_camcorder.network.packet.s2c.DynamicCaptureFrameRequestS2CP;
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
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class DynamicFrameCaptureClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final DynamicFrameUploadQueue UPLOAD_QUEUE = new DynamicFrameUploadQueue();
    private static final CaptureTemplate CAPTURE_SUPPORT = new CaptureTemplate() {
        @Override
        public Task<?> createTask(CaptureParameters params) {
            return new EmptyTask<>();
        }
    };

    private static @Nullable DynamicRecordingClientState recordingState;
    private static @Nullable String inFlightSessionId;
    private static int inFlightFrameIndex = -1;
    private static boolean keyUseWasDown;
    private static boolean freshPressPending;

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
        UPLOAD_QUEUE.startSession(packet.sessionId());
        recordingState = new DynamicRecordingClientState(packet.sessionId(), 0, packet.maxFrames(),
                packet.maxFrames(), packet.maxRecordingDurationTicks());
    }

    public static void updateState(DynamicCaptureStateS2CP packet) {
        if (!isCurrentSession(packet.sessionId())) {
            return;
        }

        UPLOAD_QUEUE.updateState(packet.sessionId(), packet.recordedFrames());
        if (packet.stopping()) {
            LOGGER.info("Server ended dynamic capture session '{}': {} frames recorded by server.",
                    packet.sessionId(), packet.recordedFrames());
            reset();
            return;
        }

        recordingState = new DynamicRecordingClientState(packet.sessionId(), packet.recordedFrames(),
                packet.recordedFrames() + packet.remainingFrames(), packet.remainingFrames(),
                packet.remainingDurationTicks());
    }

    public static void captureFrame(DynamicCaptureFrameRequestS2CP packet) {
        if (!isCurrentSession(packet.sessionId())
                || !UPLOAD_QUEUE.acceptFrameRequest(packet.sessionId(), packet.frameIndex())) {
            return;
        }
        if (packet.sessionId().equals(inFlightSessionId) && packet.frameIndex() == inFlightFrameIndex) {
            return;
        }

        inFlightSessionId = packet.sessionId();
        inFlightFrameIndex = packet.frameIndex();
        ExposureClient.cycles().enqueueTask(createCaptureTask(packet));
    }

    public static boolean hasActiveSession() {
        return UPLOAD_QUEUE.hasActiveSession();
    }

    public static void requestStop(DynamicCaptureSessionEndReason reason) {
        UPLOAD_QUEUE.requestStop(reason);
    }

    public static void reset() {
        recordingState = null;
        inFlightSessionId = null;
        inFlightFrameIndex = -1;
        freshPressPending = false;
        UPLOAD_QUEUE.clear();
    }

    public static @Nullable DynamicRecordingClientState getRecordingState() {
        return recordingState;
    }

    private static Task<?> createCaptureTask(DynamicCaptureFrameRequestS2CP packet) {
        CaptureParameters params = packet.captureParameters();
        if (params.exposureId().isEmpty()) {
            LOGGER.error("Failed to capture frame {} of session '{}': exposure id is empty.",
                    packet.frameIndex(), packet.sessionId());
            clearInFlight(packet.sessionId(), packet.frameIndex());
            return new EmptyTask<>();
        }

        @Nullable Entity entity = Minecrft.level().getEntity(params.cameraHolderId().orElse(Minecrft.player().getId()));
        if (entity == null) {
            LOGGER.error("Failed to capture frame {} of session '{}': camera holder cannot be obtained.",
                    packet.frameIndex(), packet.sessionId());
            clearInFlight(packet.sessionId(), packet.frameIndex());
            return new EmptyTask<>();
        }

        @Nullable CameraHolder holder = entity instanceof CameraHolder ? (CameraHolder) entity : null;
        Holder<ColorPalette> palette = CAPTURE_SUPPORT.getColorPalette(params);

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

        return captureTask
                .acceptAsync(exposureData -> {
                    clearInFlight(packet.sessionId(), packet.frameIndex());
                    UPLOAD_QUEUE.submitFrameData(packet.sessionId(), packet.frameIndex(), packet.exposureId(),
                            exposureData);
                })
                .onError(error -> clearInFlight(packet.sessionId(), packet.frameIndex()));
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

    private static void clearInFlight(String sessionId, int frameIndex) {
        if (sessionId.equals(inFlightSessionId) && frameIndex == inFlightFrameIndex) {
            inFlightSessionId = null;
            inFlightFrameIndex = -1;
        }
    }

    private static boolean isCurrentSession(String sessionId) {
        return recordingState != null && recordingState.sessionId().equals(sessionId);
    }
}
