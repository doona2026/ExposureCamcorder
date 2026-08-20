package io.github.exposure_camcorder.world.session;

import io.github.mortuusars.exposure.world.camera.frame.Frame;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicCaptureTickerTest {
    @Test
    void pendingFrameBlocksDuplicateCaptureRequestsUntilUploadArrives() {
        DynamicCaptureSession session = new DynamicCaptureSession("session-a", UUID.randomUUID(), 0L, 2, 3, 160);
        DynamicCaptureTicker ticker = new DynamicCaptureTicker();

        DynamicCaptureTicker.TickResult firstTick = ticker.tickSession(session, 0L);
        assertTrue(firstTick.shouldRequestFrame());

        session.markFrameRequested(0L);
        DynamicCaptureTicker.TickResult blockedTick = ticker.tickSession(session, 2L);
        assertFalse(blockedTick.shouldRequestFrame());

        session.appendFrame(Frame.EMPTY);
        DynamicCaptureTicker.TickResult secondFrameTick = ticker.tickSession(session, 2L);
        assertTrue(secondFrameTick.shouldRequestFrame());
    }

    @Test
    void releaseMovesSessionToStoppingThenFinishes() {
        DynamicCaptureSession session = new DynamicCaptureSession("session-a", UUID.randomUUID(), 100L, 2, 40, 80);
        DynamicCaptureTicker ticker = new DynamicCaptureTicker();

        ticker.tickSession(session, 100L);
        session.requestStop(DynamicCaptureSessionEndReason.RELEASED);

        DynamicCaptureTicker.TickResult result = ticker.tickSession(session, 101L);

        assertNotNull(result.completedSession());
        assertEquals(DynamicCaptureSessionEndReason.RELEASED, result.completedSession().endReason());
        assertFalse(result.completedSession().shouldCreatePhotograph());
    }

    @Test
    void timeoutMovesSessionToStoppingThenFinishes() {
        DynamicCaptureSession session = new DynamicCaptureSession("session-a", UUID.randomUUID(), 100L, 2, 40, 4);
        DynamicCaptureTicker ticker = new DynamicCaptureTicker();

        ticker.tickSession(session, 100L);
        DynamicCaptureTicker.TickResult timeoutTick = ticker.tickSession(session, 104L);

        assertFalse(timeoutTick.hasCompletedSession());
        assertTrue(session.isStopping());

        DynamicCaptureTicker.TickResult finishTick = ticker.tickSession(session, 105L);
        assertNotNull(finishTick.completedSession());
        assertEquals(DynamicCaptureSessionEndReason.TIME_LIMIT, finishTick.completedSession().endReason());
    }

    @Test
    void filmExhaustionAutomaticallyStopsAndOneFrameStillSucceeds() {
        DynamicCaptureSession session = new DynamicCaptureSession("session-a", UUID.randomUUID(), 0L, 2, 1, 80);
        DynamicCaptureTicker ticker = new DynamicCaptureTicker();

        DynamicCaptureTicker.TickResult firstTick = ticker.tickSession(session, 0L);
        assertTrue(firstTick.shouldRequestFrame());

        session.markFrameRequested(0L);
        session.appendFrame(Frame.EMPTY);

        DynamicCaptureTicker.TickResult exhaustedTick = ticker.tickSession(session, 1L);
        assertFalse(exhaustedTick.hasCompletedSession());
        assertTrue(session.isStopping());

        DynamicCaptureTicker.TickResult finishTick = ticker.tickSession(session, 2L);
        assertNotNull(finishTick.completedSession());
        assertEquals(DynamicCaptureSessionEndReason.FILM_EXHAUSTED, finishTick.completedSession().endReason());
        assertTrue(finishTick.completedSession().shouldCreatePhotograph());
        assertEquals(1, finishTick.completedSession().frameCount());
    }

    @Test
    void stoppingWaitsForPendingFrameUpload() {
        DynamicCaptureSession session = new DynamicCaptureSession("session-a", UUID.randomUUID(), 0L, 2, 40, 160);
        DynamicCaptureTicker ticker = new DynamicCaptureTicker();

        ticker.tickSession(session, 0L);
        session.markFrameRequested(0L);
        session.requestStop(DynamicCaptureSessionEndReason.RELEASED);

        DynamicCaptureTicker.TickResult waitingTick = ticker.tickSession(session, 1L);
        assertFalse(waitingTick.hasCompletedSession());

        session.appendFrame(Frame.EMPTY);

        DynamicCaptureTicker.TickResult finishTick = ticker.tickSession(session, 2L);
        assertTrue(finishTick.hasCompletedSession());
        assertEquals(1, finishTick.completedSession().frameCount());
        assertEquals(DynamicCaptureSessionEndReason.RELEASED, finishTick.completedSession().endReason());
    }

    @Test
    void delayedFinalFrameStillBecomesPartOfCompletedPhotograph() {
        DynamicCaptureSession session = new DynamicCaptureSession("session-a", UUID.randomUUID(), 0L, 2, 3, 160);
        DynamicCaptureTicker ticker = new DynamicCaptureTicker();

        ticker.tickSession(session, 0L);
        session.markFrameRequested(0L);
        session.appendFrame(Frame.EMPTY);

        DynamicCaptureTicker.TickResult secondTick = ticker.tickSession(session, 2L);
        assertTrue(secondTick.shouldRequestFrame());

        session.markFrameRequested(2L);
        session.requestStop(DynamicCaptureSessionEndReason.RELEASED);

        DynamicCaptureTicker.TickResult waitingTick = ticker.tickSession(session, 3L);
        assertFalse(waitingTick.hasCompletedSession());

        session.appendFrame(Frame.EMPTY);
        DynamicCaptureTicker.TickResult finishTick = ticker.tickSession(session, 4L);
        assertTrue(finishTick.hasCompletedSession());
        assertEquals(2, finishTick.completedSession().frameCount());
        assertTrue(finishTick.completedSession().shouldCreatePhotograph());
    }

    @Test
    void timedOutPendingFrameRequestsAreRetriedBeforeSessionStops() {
        DynamicCaptureSession session = new DynamicCaptureSession("session-a", UUID.randomUUID(), 0L, 2, 40, 160);
        DynamicCaptureTicker ticker = new DynamicCaptureTicker(5, 1);

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
        assertTrue(finishTick.hasCompletedSession());
        assertEquals(DynamicCaptureSessionEndReason.INTERRUPTED, finishTick.completedSession().endReason());
    }
}
