package io.github.exposure_camcorder.compatibility.exposure;

import io.github.exposure_camcorder.ExposureCamcorder;
import io.github.exposure_camcorder.world.session.DynamicCaptureSession;
import io.github.exposure_camcorder.world.session.DynamicCaptureSessionEndReason;
import io.github.mortuusars.exposure.ExposureServer;
import io.github.mortuusars.exposure.world.camera.frame.Frame;
import io.github.mortuusars.exposure.world.level.storage.ExposureData;
import io.github.mortuusars.exposure.world.level.storage.ExposureIdentifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class ExposureAccess {
    public static boolean receiveFrameData(ServerPlayer player, String sessionId, int frameIndex, String exposureId,
                                           ExposureData exposureData) {
        Optional<DynamicCaptureSession> sessionOpt = ExposureCamcorder.captureSessionManager().getActiveSession(player.getUUID());
        if (sessionOpt.isEmpty()) {
            ExposureCamcorder.LOGGER.warn("Discarding dynamic frame upload without active session. Player='{}'.",
                    player.getScoreboardName());
            return false;
        }

        DynamicCaptureSession session = sessionOpt.get();
        if (!session.sessionId().equals(sessionId)) {
            ExposureCamcorder.LOGGER.warn("Discarding dynamic frame upload for mismatched session '{}'. Expected '{}'.",
                    sessionId, session.sessionId());
            return false;
        }

        if (frameIndex != session.frameCount()) {
            ExposureCamcorder.LOGGER.warn("Discarding out-of-order dynamic frame {} for session '{}'. Expected '{}'.",
                    frameIndex, sessionId, session.frameCount());
            return false;
        }

        String expectedExposureId = session.pendingFrameUploadId();
        if (!expectedExposureId.equals(exposureId)) {
            ExposureCamcorder.LOGGER.warn(
                    "Discarding dynamic frame {} for session '{}' with mismatched exposure id '{}'. Expected '{}'.",
                    frameIndex, sessionId, exposureId, expectedExposureId);
            return false;
        }

        if (!storeExposure(expectedExposureId, exposureData)) {
            return false;
        }

        ExposureCamcorder.captureSessionManager()
                .appendFrame(player.getUUID(), createFrame(expectedExposureId), player.level().getGameTime());
        return true;
    }

    public static boolean requestStop(ServerPlayer player, String sessionId, DynamicCaptureSessionEndReason reason) {
        Optional<DynamicCaptureSession> sessionOpt = ExposureCamcorder.captureSessionManager().getActiveSession(player.getUUID());
        if (sessionOpt.isEmpty()) {
            return false;
        }

        DynamicCaptureSession session = sessionOpt.get();
        if (!session.sessionId().equals(sessionId)) {
            return false;
        }

        session.requestStop(reason);
        return true;
    }

    public static Frame createFrame(String exposureId) {
        return Frame.EMPTY.toMutable()
                .setIdentifier(ExposureIdentifier.id(exposureId))
                .toImmutable();
    }

    public static String createExposureId(String sessionId, int frameIndex) {
        return createExposureId(sessionId, frameIndex, 0);
    }

    public static String createExposureId(String sessionId, int frameIndex, int attempt) {
        if (attempt <= 0) {
            return ExposureIdentifier.createId(sessionId, "dynamic", Integer.toString(frameIndex));
        }

        return ExposureIdentifier.createId(sessionId, "dynamic", Integer.toString(frameIndex),
                "retry", Integer.toString(attempt));
    }

    public static Optional<ExposureData> loadExposure(String exposureId) {
        return ExposureServer.exposureRepository().load(exposureId).getData();
    }

    private static boolean storeExposure(String exposureId, ExposureData exposureData) {
        ExposureServer.exposureRepository().save(exposureId, exposureData);
        return loadExposure(exposureId).isPresent();
    }
}
