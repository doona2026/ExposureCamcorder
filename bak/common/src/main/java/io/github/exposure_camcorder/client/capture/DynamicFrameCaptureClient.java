package io.github.exposure_camcorder.client.capture;

import com.mojang.logging.LogUtils;
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
import io.github.mortuusars.exposure.util.cycles.task.Result;
import io.github.mortuusars.exposure.util.cycles.task.EmptyTask;
import io.github.mortuusars.exposure.util.cycles.task.Task;
import io.github.mortuusars.exposure.world.camera.capture.CaptureParameters;
import io.github.mortuusars.exposure.world.camera.capture.Projection;
import io.github.mortuusars.exposure.world.entity.CameraHolder;
import io.github.mortuusars.exposure.world.level.storage.ExposureData;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class DynamicFrameCaptureClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final DynamicFrameUploadQueue UPLOAD_QUEUE = new DynamicFrameUploadQueue();
    private static @Nullable DynamicRecordingClientState recordingState;
    private static final CaptureTemplate CAPTURE_SUPPORT = new CaptureTemplate() {
        @Override
        public Task<?> createTask(CaptureParameters params) {
            return new EmptyTask<>();
        }
    };

    public static void init() {
    }

    public static void startSession(DynamicCaptureStartS2CP packet) {
        UPLOAD_QUEUE.startSession(packet.sessionId());
        recordingState = new DynamicRecordingClientState(packet.sessionId(), 0, packet.maxFrames(),
                packet.maxFrames(), packet.maxRecordingDurationTicks());
    }

    public static void updateState(DynamicCaptureStateS2CP packet) {
        UPLOAD_QUEUE.updateState(packet.sessionId(), packet.recordedFrames());
        if (packet.stopping()) {
            recordingState = null;
            UPLOAD_QUEUE.finishSession(packet.sessionId());
            return;
        }

        recordingState = new DynamicRecordingClientState(packet.sessionId(), packet.recordedFrames(),
                packet.recordedFrames() + packet.remainingFrames(), packet.remainingFrames(), packet.remainingDurationTicks());
    }

    public static void captureFrame(DynamicCaptureFrameRequestS2CP packet) {
        ExposureClient.cycles().enqueueTask(createCaptureTask(packet));
    }

    public static boolean hasActiveSession() {
        return UPLOAD_QUEUE.hasActiveSession();
    }

    public static void reset() {
        recordingState = null;
        UPLOAD_QUEUE.clear();
    }

    public static void requestStop(DynamicCaptureSessionEndReason reason) {
        UPLOAD_QUEUE.requestStop(reason);
    }

    public static @Nullable DynamicRecordingClientState getRecordingState() {
        return recordingState;
    }

    private static Task<?> createCaptureTask(DynamicCaptureFrameRequestS2CP packet) {
        CaptureParameters params = packet.captureParameters();
        if (params.exposureId().isEmpty()) {
            LOGGER.error("Failed to capture frame {} of session '{}': exposure id is empty.",
                    packet.frameIndex(), packet.sessionId());
            return new EmptyTask<>();
        }

        @Nullable Entity entity = Minecrft.level().getEntity(params.cameraHolderId().orElse(Minecrft.player().getId()));
        if (entity == null) {
            LOGGER.error("Failed to capture frame {} of session '{}': camera holder cannot be obtained.",
                    packet.frameIndex(), packet.sessionId());
            return new EmptyTask<>();
        }

        @Nullable CameraHolder holder = entity instanceof CameraHolder cameraHolder ? cameraHolder : null;

        Holder<ColorPalette> palette = CAPTURE_SUPPORT.getColorPalette(params);

        Task<ExposureData> captureTask = Capture.of(createScreenshotTask(),
                        CaptureAction.setCameraEntity(entity),
                        CaptureAction.forceRegularOrSelfieCamera(holder),
                        CaptureAction.optional(params.fov(), CaptureAction::setFov),
                        CaptureAction.optional(Config.Client.KEEP_POST_EFFECT.isFalse(), CaptureAction::disablePostEffect),
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
                    .then(CAPTURE_SUPPORT.convertToExposureData(palette, CAPTURE_SUPPORT.createExposureTag(params, true))));
        }

        return captureTask
                .acceptAsync(exposureData -> UPLOAD_QUEUE.submitFrameData(packet.sessionId(),
                        packet.frameIndex(), packet.exposureId(), exposureData))
                .onError(CAPTURE_SUPPORT.printCasualErrorInChat());
    }

    private static Task<Result<Image>> createScreenshotTask() {
        return ExposureClient.shouldUseDirectCapture()
                ? new ViewfinderDirectScreenshotCaptureTask()
                : new BackgroundScreenshotCaptureTask();
    }
}
