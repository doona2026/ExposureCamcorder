package io.github.exposure_camcorder.world.session;

public class DynamicCaptureTicker {
    private final int pendingFrameTimeoutTicks;
    private final int maxPendingFrameRetries;

    public DynamicCaptureTicker() {
        this(200, 4);
    }

    public DynamicCaptureTicker(int pendingFrameTimeoutTicks, int maxPendingFrameRetries) {
        if (pendingFrameTimeoutTicks <= 0) {
            throw new IllegalArgumentException("pendingFrameTimeoutTicks must be positive.");
        }
        if (maxPendingFrameRetries < 0) {
            throw new IllegalArgumentException("maxPendingFrameRetries cannot be negative.");
        }

        this.pendingFrameTimeoutTicks = pendingFrameTimeoutTicks;
        this.maxPendingFrameRetries = maxPendingFrameRetries;
    }

    public record TickResult(boolean shouldRequestFrame, boolean shouldRetryPendingFrame,
                             DynamicCaptureSessionResult completedSession) {
        public boolean hasCompletedSession() {
            return completedSession != null;
        }
    }

    public TickResult tickSession(DynamicCaptureSession session, long currentTick) {
        if (session.state() == DynamicCaptureSession.State.FINISHED) {
            return new TickResult(false, false, null);
        }

        if (session.state() == DynamicCaptureSession.State.STARTING) {
            session.beginRecording();
        }

        if (session.state() == DynamicCaptureSession.State.STOPPING) {
            if (session.hasPendingFrameUpload()) {
                if (session.hasPendingFrameTimedOut(currentTick, pendingFrameTimeoutTicks)) {
                    if (session.pendingFrameRetryCount() < maxPendingFrameRetries) {
                        return new TickResult(false, true, null);
                    }
                    return new TickResult(false, false, session.finish(DynamicCaptureSessionEndReason.INVALIDATED));
                }
                return new TickResult(false, false, null);
            }
            return new TickResult(false, false, session.finish(DynamicCaptureSessionEndReason.INVALIDATED));
        }

        if (session.hasPendingFrameUpload()) {
            if (session.hasPendingFrameTimedOut(currentTick, pendingFrameTimeoutTicks)) {
                if (session.pendingFrameRetryCount() < maxPendingFrameRetries) {
                    return new TickResult(false, true, null);
                }
                session.requestStop(DynamicCaptureSessionEndReason.INTERRUPTED);
            }
            return new TickResult(false, false, null);
        }

        if (session.hasTimedOut(currentTick)) {
            session.requestStop(DynamicCaptureSessionEndReason.TIME_LIMIT);
            return new TickResult(false, false, null);
        }

        if (session.isFilmExhausted()) {
            session.requestStop(DynamicCaptureSessionEndReason.FILM_EXHAUSTED);
            return new TickResult(false, false, null);
        }

        return new TickResult(session.shouldCaptureOnTick(currentTick), false, null);
    }
}
