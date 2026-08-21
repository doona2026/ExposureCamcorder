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
        return new DynamicCaptureSession("session-a", UUID.randomUUID(), startTick, interval, maxFrames, maxDurationTicks);
    }

    @Test
    void startingSessionBeginsRecordingOnFirstTick() {
        DynamicCaptureSession session = session(0, 2, 40, 160);
        DynamicCaptureTicker ticker = new DynamicCaptureTicker();

        DynamicCaptureTicker.TickResult result = ticker.tickSession(session, 0L);

        assertNull(result.completedSession());
        assertTrue(session.isRecording());
    }

    @Test
    void timeoutMovesSessionToStoppingThenFinishesWithTimeLimit() {
        DynamicCaptureSession session = session(100, 2, 40, 80);
        DynamicCaptureTicker ticker = new DynamicCaptureTicker();

        ticker.tickSession(session, 100L);
        DynamicCaptureTicker.TickResult timeoutTick = ticker.tickSession(session, 200L);

        assertNull(timeoutTick.completedSession());
        assertTrue(session.isStopping());

        DynamicCaptureTicker.TickResult finishTick = ticker.tickSession(session, 299L);
        assertNull(finishTick.completedSession());

        finishTick = ticker.tickSession(session, 300L);
        assertNotNull(finishTick.completedSession());
        assertEquals(DynamicCaptureSessionEndReason.TIME_LIMIT, finishTick.completedSession().endReason());
    }

    @Test
    void filmExhaustionAutomaticallyStopsAndOneFrameStillSucceeds() {
        DynamicCaptureSession session = session(0, 2, 2, 1000);
        DynamicCaptureTicker ticker = new DynamicCaptureTicker();

        ticker.tickSession(session, 0L);
        session.appendFrame(Frame.EMPTY);
        session.appendFrame(Frame.EMPTY);
        session.markFrameReceived(2L);

        DynamicCaptureTicker.TickResult exhaustedTick = ticker.tickSession(session, 3L);
        assertNull(exhaustedTick.completedSession());
        assertTrue(session.isStopping());

        DynamicCaptureTicker.TickResult finishTick = ticker.tickSession(session, 104L);
        assertNotNull(finishTick.completedSession());
        assertEquals(DynamicCaptureSessionEndReason.FILM_EXHAUSTED, finishTick.completedSession().endReason());
        assertTrue(finishTick.completedSession().shouldCreatePhotograph());
        assertEquals(2, finishTick.completedSession().frameCount());
    }

    @Test
    void stalledSessionIsInterruptedWhenNoFramesArrive() {
        DynamicCaptureSession session = session(0, 2, 40, 10000);
        DynamicCaptureTicker ticker = new DynamicCaptureTicker(200, 100);

        ticker.tickSession(session, 0L);

        DynamicCaptureTicker.TickResult beforeStall = ticker.tickSession(session, 199L);
        assertNull(beforeStall.completedSession());
        assertTrue(session.isRecording());

        DynamicCaptureTicker.TickResult stalledTick = ticker.tickSession(session, 200L);
        assertNull(stalledTick.completedSession());
        assertTrue(session.isStopping());

        DynamicCaptureTicker.TickResult finishTick = ticker.tickSession(session, 301L);
        assertNotNull(finishTick.completedSession());
        assertEquals(DynamicCaptureSessionEndReason.INTERRUPTED, finishTick.completedSession().endReason());
    }

    @Test
    void framesReceivedResetTheStallTimer() {
        DynamicCaptureSession session = session(0, 2, 40, 10000);
        DynamicCaptureTicker ticker = new DynamicCaptureTicker(200, 100);

        ticker.tickSession(session, 0L);
        session.markFrameReceived(150L);

        DynamicCaptureTicker.TickResult result = ticker.tickSession(session, 199L);
        assertNull(result.completedSession());
        assertTrue(session.isRecording());
    }

    @Test
    void clientActivityResetsTheStallTimerWhileHighResolutionFrameIsProcessing() {
        DynamicCaptureSession session = session(0, 2, 600, 10000);
        DynamicCaptureTicker ticker = new DynamicCaptureTicker(200, 100);

        ticker.tickSession(session, 0L);
        session.markClientActivity(350L);

        DynamicCaptureTicker.TickResult beforeStall = ticker.tickSession(session, 549L);
        assertNull(beforeStall.completedSession());
        assertTrue(session.isRecording());

        DynamicCaptureTicker.TickResult stalledTick = ticker.tickSession(session, 550L);
        assertNull(stalledTick.completedSession());
        assertTrue(session.isStopping());
    }

    @Test
    void framesArrivingDuringGraceAreIncludedInThePartialRecording() {
        DynamicCaptureSession session = session(0, 2, 40, 1000);
        DynamicCaptureTicker ticker = new DynamicCaptureTicker();

        ticker.tickSession(session, 0L);
        session.requestStop(DynamicCaptureSessionEndReason.RELEASED);

        DynamicCaptureTicker.TickResult waitingTick = ticker.tickSession(session, 1L);
        assertNull(waitingTick.completedSession());

        session.appendFrame(Frame.EMPTY);
        session.markFrameReceived(5L);

        DynamicCaptureTicker.TickResult finishTick = ticker.tickSession(session, 101L);
        assertNotNull(finishTick.completedSession());
        assertEquals(DynamicCaptureSessionEndReason.RELEASED, finishTick.completedSession().endReason());
        assertEquals(1, finishTick.completedSession().frameCount());
        assertFalse(finishTick.completedSession().shouldCreatePhotograph());
    }

    @Test
    void zeroFrameSessionFinishesAsFailure() {
        DynamicCaptureSession session = session(0, 2, 40, 1000);
        DynamicCaptureTicker ticker = new DynamicCaptureTicker();

        ticker.tickSession(session, 0L);
        session.requestStop(DynamicCaptureSessionEndReason.RELEASED);

        DynamicCaptureTicker.TickResult finishTick = ticker.tickSession(session, 100L);
        assertNull(finishTick.completedSession());

        finishTick = ticker.tickSession(session, 200L);
        assertNotNull(finishTick.completedSession());
        assertEquals(DynamicCaptureSessionEndReason.RELEASED, finishTick.completedSession().endReason());
        assertFalse(finishTick.completedSession().shouldCreatePhotograph());
        assertEquals(0, finishTick.completedSession().frameCount());
    }
}
