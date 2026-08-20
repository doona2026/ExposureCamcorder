package io.github.exposure_camcorder.world.session;

import io.github.exposure_camcorder.util.DynamicPhotographValidation;
import io.github.mortuusars.exposure.world.camera.frame.Frame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class DynamicCaptureSession {
    public enum State {
        STARTING,
        RECORDING,
        STOPPING,
        FINISHED
    }

    private final String sessionId;
    private final UUID playerId;
    private final long startTick;
    private final int captureIntervalTicks;
    private final int maxFrames;
    private final int maxRecordingDurationTicks;
    private final List<Frame> frames;
    private final List<Frame> framesView;

    private State state = State.STARTING;
    private DynamicCaptureSessionEndReason endReason;
    private int pendingFrameIndex = -1;
    private long pendingFrameRequestedAtTick = -1L;
    private int pendingFrameRetryCount = 0;

    public DynamicCaptureSession(String sessionId, UUID playerId, long startTick, int captureIntervalTicks,
                                 int maxFrames, int maxRecordingDurationTicks) {
        this.sessionId = sessionId;
        this.playerId = playerId;
        this.startTick = startTick;
        this.captureIntervalTicks = DynamicPhotographValidation.validateCaptureInterval(captureIntervalTicks);
        this.maxFrames = DynamicPhotographValidation.validateMaxFrames(maxFrames);
        if (maxRecordingDurationTicks <= 0) {
            throw new IllegalArgumentException("maxRecordingDurationTicks must be positive.");
        }
        this.maxRecordingDurationTicks = maxRecordingDurationTicks;
        this.frames = new ArrayList<>(maxFrames);
        this.framesView = Collections.unmodifiableList(frames);
    }

    public String sessionId() {
        return sessionId;
    }

    public UUID playerId() {
        return playerId;
    }

    public long startTick() {
        return startTick;
    }

    public int captureIntervalTicks() {
        return captureIntervalTicks;
    }

    public int maxFrames() {
        return maxFrames;
    }

    public int maxRecordingDurationTicks() {
        return maxRecordingDurationTicks;
    }

    public State state() {
        return state;
    }

    public List<Frame> frames() {
        return framesView;
    }

    public int frameCount() {
        return frames.size();
    }

    public DynamicCaptureSessionEndReason endReason() {
        return endReason;
    }

    public int pendingFrameIndex() {
        return pendingFrameIndex;
    }

    public int pendingFrameRetryCount() {
        return pendingFrameRetryCount;
    }

    public boolean isActive() {
        return state != State.FINISHED;
    }

    public boolean isRecording() {
        return state == State.RECORDING;
    }

    public boolean isStopping() {
        return state == State.STOPPING;
    }

    public void beginRecording() {
        if (state == State.STARTING) {
            state = State.RECORDING;
        }
    }

    public boolean canAppendFrame() {
        return (state == State.RECORDING || hasPendingFrameUpload()) && frames.size() < maxFrames;
    }

    public void appendFrame(Frame frame) {
        if (!canAppendFrame()) {
            throw new IllegalStateException("Session cannot accept more frames in state " + state);
        }
        frames.add(frame);
        if (pendingFrameIndex == frames.size() - 1) {
            pendingFrameIndex = -1;
            pendingFrameRequestedAtTick = -1L;
            pendingFrameRetryCount = 0;
        }
    }

    public long elapsedTicks(long currentTick) {
        return Math.max(0L, currentTick - startTick);
    }

    public boolean hasTimedOut(long currentTick) {
        return elapsedTicks(currentTick) >= maxRecordingDurationTicks;
    }

    public boolean isFilmExhausted() {
        return frames.size() >= maxFrames;
    }

    public boolean shouldCaptureOnTick(long currentTick) {
        if (state != State.RECORDING || isFilmExhausted() || hasPendingFrameUpload()) {
            return false;
        }
        long scheduledTick = startTick + (long) frames.size() * captureIntervalTicks;
        return currentTick >= scheduledTick;
    }

    public void requestStop(DynamicCaptureSessionEndReason reason) {
        if (state == State.FINISHED) {
            return;
        }
        endReason = reason;
        state = State.STOPPING;
    }

    public void markFrameRequested(long currentTick) {
        pendingFrameIndex = frames.size();
        pendingFrameRequestedAtTick = currentTick;
        pendingFrameRetryCount = 0;
    }

    public void markPendingFrameRetried(long currentTick) {
        if (!hasPendingFrameUpload()) {
            throw new IllegalStateException("No pending frame upload to retry.");
        }

        pendingFrameRequestedAtTick = currentTick;
        pendingFrameRetryCount++;
    }

    public boolean hasPendingFrameUpload() {
        return pendingFrameIndex >= frames.size() && pendingFrameIndex < maxFrames;
    }

    public boolean hasPendingFrameTimedOut(long currentTick, int timeoutTicks) {
        if (!hasPendingFrameUpload() || timeoutTicks <= 0 || pendingFrameRequestedAtTick < 0L) {
            return false;
        }

        return currentTick - pendingFrameRequestedAtTick >= timeoutTicks;
    }

    public DynamicCaptureSessionResult finish(DynamicCaptureSessionEndReason fallbackReason) {
        if (endReason == null) {
            endReason = fallbackReason;
        }
        state = State.FINISHED;
        return new DynamicCaptureSessionResult(sessionId, endReason, framesView,
                DynamicPhotographValidation.canCreatePhotograph(frames.size()));
    }
}
