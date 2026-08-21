package io.github.exposure_camcorder.world.session;

public class DynamicCaptureTicker {
    private final int maxPendingFrameRetries;

    public DynamicCaptureTicker() {
        this(4);
    }

    public DynamicCaptureTicker(int maxPendingFrameRetries) {
        if (maxPendingFrameRetries < 0) {
            throw new IllegalArgumentException("maxPendingFrameRetries cannot be negative.");
        }
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
                if (session.hasPendingFrameTimedOut(currentTick)) {
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
            if (session.hasPendingFrameTimedOut(currentTick)) {
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
