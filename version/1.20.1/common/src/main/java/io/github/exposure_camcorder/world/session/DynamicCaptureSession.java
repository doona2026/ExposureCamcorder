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
    private long lastFrameReceivedTick;
    private long stopRequestedTick = -1L;

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
        this.lastFrameReceivedTick = startTick;
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
        return (state == State.RECORDING || state == State.STOPPING) && frames.size() < maxFrames;
    }

    public void appendFrame(Frame frame) {
        if (!canAppendFrame()) {
            throw new IllegalStateException("Session cannot accept more frames in state " + state);
        }
        frames.add(frame);
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

    public void markFrameReceived(long currentTick) {
        lastFrameReceivedTick = currentTick;
    }

    public boolean hasStalled(long currentTick, int stallTicks) {
        if (state != State.RECORDING || stallTicks <= 0) {
            return false;
        }
        return currentTick - lastFrameReceivedTick >= stallTicks;
    }

    public void requestStop(DynamicCaptureSessionEndReason reason) {
        requestStop(reason, -1L);
    }

    public void requestStop(DynamicCaptureSessionEndReason reason, long currentTick) {
        if (state == State.FINISHED) {
            return;
        }
        endReason = reason;
        state = State.STOPPING;
        if (stopRequestedTick < 0L && currentTick >= 0L) {
            stopRequestedTick = currentTick;
        }
    }

    public boolean stopGraceElapsed(long currentTick, int graceTicks) {
        if (state != State.STOPPING) {
            return false;
        }
        if (stopRequestedTick < 0L) {
            stopRequestedTick = currentTick;
        }
        return graceTicks > 0 && currentTick - stopRequestedTick >= graceTicks;
    }

    public DynamicCaptureSessionResult finish(DynamicCaptureSessionEndReason fallbackReason) {
        if (endReason == null) {
            endReason = fallbackReason;
        }
        state = State.FINISHED;
        return new DynamicCaptureSessionResult(sessionId, endReason, framesView,
                frames.size() >= maxFrames);
    }
}
