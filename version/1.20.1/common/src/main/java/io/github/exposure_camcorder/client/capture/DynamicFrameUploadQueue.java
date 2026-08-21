package io.github.exposure_camcorder.client.capture;

import io.github.exposure_camcorder.ExposureCamcorder;
import io.github.exposure_camcorder.network.Packets;
import io.github.exposure_camcorder.network.packet.c2s.DynamicCaptureFrameDataC2SP;
import io.github.exposure_camcorder.network.packet.c2s.DynamicCaptureStopC2SP;
import io.github.exposure_camcorder.world.session.DynamicCaptureSessionEndReason;
import io.github.mortuusars.exposure.world.level.storage.ExposureData;
import org.jetbrains.annotations.Nullable;

public class DynamicFrameUploadQueue {
    @FunctionalInterface
    interface FrameDataSender {
        void send(String sessionId, int frameIndex, String exposureId, ExposureData exposureData);
    }

    @FunctionalInterface
    interface StopSender {
        void send(String sessionId, DynamicCaptureSessionEndReason reason);
    }

    private final FrameDataSender frameDataSender;
    private final StopSender stopSender;
    private @Nullable String activeSessionId;
    private int acknowledgedFrameCount;
    private boolean stopRequested;

    public DynamicFrameUploadQueue() {
        this((sessionId, frameIndex, exposureId, exposureData) ->
                        Packets.sendToServer(new DynamicCaptureFrameDataC2SP(sessionId, frameIndex, exposureId, exposureData)),
                (sessionId, reason) -> Packets.sendToServer(new DynamicCaptureStopC2SP(sessionId, reason)));
    }

    DynamicFrameUploadQueue(FrameDataSender frameDataSender, StopSender stopSender) {
        this.frameDataSender = frameDataSender;
        this.stopSender = stopSender;
    }

    public void startSession(String sessionId) {
        activeSessionId = sessionId;
        acknowledgedFrameCount = 0;
        stopRequested = false;
    }

    public void updateState(String sessionId, int recordedFrames) {
        if (matchesSession(sessionId)) {
            acknowledgedFrameCount = Math.max(acknowledgedFrameCount, recordedFrames);
        }
    }

    public void finishSession(String sessionId) {
        if (matchesSession(sessionId)) {
            clearState();
        }
    }

    public boolean submitFrameData(String sessionId, int frameIndex, String exposureId, ExposureData exposureData) {
        if (!matchesSession(sessionId)) {
            ExposureCamcorder.LOGGER.warn("Ignoring frame upload for inactive session '{}'.", sessionId);
            return false;
        }

        if (stopRequested || frameIndex < acknowledgedFrameCount) {
            return false;
        }

        frameDataSender.send(sessionId, frameIndex, exposureId, exposureData);
        return true;
    }

    public void requestStop(String sessionId, DynamicCaptureSessionEndReason reason) {
        if (!matchesSession(sessionId) || stopRequested) {
            return;
        }

        stopRequested = true;
        stopSender.send(sessionId, reason);
    }

    public void requestStop(DynamicCaptureSessionEndReason reason) {
        if (activeSessionId != null) {
            requestStop(activeSessionId, reason);
        }
    }

    public boolean hasActiveSession() {
        return activeSessionId != null && !stopRequested;
    }

    public void clear() {
        clearState();
    }

    private boolean matchesSession(String sessionId) {
        return activeSessionId != null && activeSessionId.equals(sessionId);
    }

    private void clearState() {
        activeSessionId = null;
        acknowledgedFrameCount = 0;
        stopRequested = false;
    }
}
