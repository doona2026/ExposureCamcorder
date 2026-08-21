package io.github.exposure_camcorder.world.session;

public class DynamicCaptureTicker {
    private final int stoppingGraceTicks;

    public DynamicCaptureTicker() {
        this(100);
    }

    public DynamicCaptureTicker(int stoppingGraceTicks) {
        if (stoppingGraceTicks < 0) {
            throw new IllegalArgumentException("stoppingGraceTicks cannot be negative.");
        }

        this.stoppingGraceTicks = stoppingGraceTicks;
    }

    public record TickResult(DynamicCaptureSessionResult completedSession) {
        public boolean hasCompletedSession() {
            return completedSession != null;
        }
    }

    public TickResult tickSession(DynamicCaptureSession session, long currentTick) {
        if (session.state() == DynamicCaptureSession.State.FINISHED) {
            return new TickResult(null);
        }

        if (session.state() == DynamicCaptureSession.State.STARTING) {
            session.beginRecording();
        }

        if (session.state() == DynamicCaptureSession.State.STOPPING) {
            if (session.stopGraceElapsed(currentTick, stoppingGraceTicks)) {
                return new TickResult(session.finish(DynamicCaptureSessionEndReason.INVALIDATED));
            }
            return new TickResult(null);
        }

        if (session.hasTimedOut(currentTick)) {
            session.requestStop(DynamicCaptureSessionEndReason.TIME_LIMIT, currentTick);
        } else if (session.isFilmExhausted()) {
            session.requestStop(DynamicCaptureSessionEndReason.FILM_EXHAUSTED, currentTick);
        } else if (session.hasStalled(currentTick)) {
            session.requestStop(DynamicCaptureSessionEndReason.INTERRUPTED, currentTick);
        }
        return new TickResult(null);
    }
}
