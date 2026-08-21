package io.github.exposure_camcorder.client.capture;

public record DynamicRecordingClientState(String sessionId, int recordedFrames, int maxFrames,
                                          int remainingFrames, int remainingDurationTicks) {
    public DynamicRecordingClientState {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank.");
        }
        if (recordedFrames < 0 || maxFrames < 0 || remainingFrames < 0 || remainingDurationTicks < 0) {
            throw new IllegalArgumentException("recording state values must be non-negative.");
        }
    }

    public boolean isActive() {
        return maxFrames > 0;
    }
}
