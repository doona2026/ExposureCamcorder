package io.github.exposure_camcorder.world.session;

import io.github.mortuusars.exposure.world.camera.frame.Frame;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicCaptureTickerTest {
    private static DynamicCaptureSession session(int startTick, int interval, int maxFrames, int maxDurationTicks) {
        return new DynamicCaptureSession("session-a", UUID.randomUUID(), startTick, interval, maxFrames,
                maxDurationTicks);
    }

    private static DynamicCaptureSession session(int startTick, int interval, int maxFrames, int maxDurationTicks,
                                                 int pendingFrameTimeoutTicks) {
        return new DynamicCaptureSession("session-a", UUID.randomUUID(), startTick, interval, maxFrames,
                maxDurationTicks, pendingFrameTimeoutTicks);
    }

    @Test
    void startingSessionBeginsRecordingAndRequestsFirstFrame() {
        DynamicCaptureSession session = session(0, 2, 40, 160);
        DynamicCaptureTicker ticker = new DynamicCaptureTicker();

        DynamicCaptureTicker.TickResult result = ticker.tickSession(session, 0L);

        assertNull(result.completedSession());
        assertTrue(session.isRecording());
        assertTrue(result.shouldRequestFrame());
    }

    @Test
    void pendingFrameBlocksDuplicateCaptureRequestsUntilUploadArrives() {
        DynamicCaptureSession session = session(0, 2, 3, 160);
        DynamicCaptureTicker ticker = new DynamicCaptureTicker();

        assertTrue(ticker.tickSession(session, 0L).shouldRequestFrame());

        session.markFrameRequested(0L);
        assertFalse(ticker.tickSession(session, 2L).shouldRequestFrame());

        session.appendFrame(Frame.EMPTY);
        assertTrue(ticker.tickSession(session, 2L).shouldRequestFrame());
    }

    @Test
    void timedOutPendingFrameRequestsAreRetriedBeforeSessionStops() {
        DynamicCaptureSession session = session(0, 2, 40, 160, 5);
        DynamicCaptureTicker ticker = new DynamicCaptureTicker(1);

        ticker.tickSession(session, 0L);
        session.markFrameRequested(0L);

        DynamicCaptureTicker.TickResult retryTick = ticker.tickSession(session, 5L);
        assertTrue(retryTick.shouldRetryPendingFrame());
        assertFalse(retryTick.hasCompletedSession());

        session.markPendingFrameRetried(5L);
        DynamicCaptureTicker.TickResult stopTick = ticker.tickSession(session, 10L);
        assertFalse(stopTick.shouldRetryPendingFrame());
        assertTrue(session.isStopping());

        DynamicCaptureTicker.TickResult finishTick = ticker.tickSession(session, 11L);
        assertNotNull(finishTick.completedSession());
        assertEquals(DynamicCaptureSessionEndReason.INTERRUPTED, finishTick.completedSession().endReason());
    }

    @Test
    void highResolutionPendingTimeoutSurvivesLongTickWithoutRetry() {
        DynamicCaptureSession session = session(0, 2, 600, 10000, 1800);
        DynamicCaptureTicker ticker = new DynamicCaptureTicker();

        ticker.tickSession(session, 0L);
        session.markFrameRequested(0L);

        DynamicCaptureTicker.TickResult result = ticker.tickSession(session, 1200L);
        assertFalse(result.shouldRetryPendingFrame());
        assertFalse(result.hasCompletedSession());
        assertTrue(session.isRecording());
    }

    @Test
    void timeoutMovesSessionToStoppingThenFinishesWithTimeLimit() {
        DynamicCaptureSession session = session(100, 2, 40, 80);
        DynamicCaptureTicker ticker = new DynamicCaptureTicker();

        ticker.tickSession(session, 100L);
        DynamicCaptureTicker.TickResult timeoutTick = ticker.tickSession(session, 180L);

        assertFalse(timeoutTick.hasCompletedSession());
        assertTrue(session.isStopping());

        DynamicCaptureTicker.TickResult finishTick = ticker.tickSession(session, 181L);
        assertNotNull(finishTick.completedSession());
        assertEquals(DynamicCaptureSessionEndReason.TIME_LIMIT, finishTick.completedSession().endReason());
    }

    @Test
    void stoppingWaitsForPendingFrameUploadAndIncludesIt() {
        DynamicCaptureSession session = session(0, 2, 40, 160);
        DynamicCaptureTicker ticker = new DynamicCaptureTicker();

        ticker.tickSession(session, 0L);
        session.markFrameRequested(0L);
        session.requestStop(DynamicCaptureSessionEndReason.RELEASED);

        DynamicCaptureTicker.TickResult waitingTick = ticker.tickSession(session, 1L);
        assertFalse(waitingTick.hasCompletedSession());

        session.appendFrame(Frame.EMPTY);
        DynamicCaptureTicker.TickResult finishTick = ticker.tickSession(session, 2L);
        assertNotNull(finishTick.completedSession());
        assertEquals(1, finishTick.completedSession().frameCount());
        assertEquals(DynamicCaptureSessionEndReason.RELEASED, finishTick.completedSession().endReason());
    }

    @Test
    void filmExhaustionFinalizesAsCompletedPhotograph() {
        DynamicCaptureSession session = session(0, 2, 2, 1000);
        DynamicCaptureTicker ticker = new DynamicCaptureTicker();

        assertTrue(ticker.tickSession(session, 0L).shouldRequestFrame());
        session.markFrameRequested(0L);
        session.appendFrame(Frame.EMPTY);

        assertTrue(ticker.tickSession(session, 2L).shouldRequestFrame());
        session.markFrameRequested(2L);
        session.appendFrame(Frame.EMPTY);

        DynamicCaptureTicker.TickResult exhaustedTick = ticker.tickSession(session, 3L);
        assertFalse(exhaustedTick.hasCompletedSession());
        assertTrue(session.isStopping());

        DynamicCaptureTicker.TickResult finishTick = ticker.tickSession(session, 4L);
        assertNotNull(finishTick.completedSession());
        assertEquals(DynamicCaptureSessionEndReason.FILM_EXHAUSTED, finishTick.completedSession().endReason());
        assertTrue(finishTick.completedSession().shouldCreatePhotograph());
        assertEquals(2, finishTick.completedSession().frameCount());
    }
}
