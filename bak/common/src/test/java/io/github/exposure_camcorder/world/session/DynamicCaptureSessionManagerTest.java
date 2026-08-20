package io.github.exposure_camcorder.world.session;

import io.github.mortuusars.exposure.world.camera.frame.Frame;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicCaptureSessionManagerTest {
    @Test
    void samePlayerCannotStartTwoActiveSessions() {
        DynamicCaptureSessionManager manager = new DynamicCaptureSessionManager();
        UUID playerId = UUID.randomUUID();

        manager.startSession(playerId, "session-a", 0L, 2, 40, 80);

        assertThrows(IllegalStateException.class,
                () -> manager.startSession(playerId, "session-b", 0L, 2, 40, 80));
    }

    @Test
    void zeroFrameSessionFinishesAsFailure() {
        DynamicCaptureSessionManager manager = new DynamicCaptureSessionManager();
        UUID playerId = UUID.randomUUID();

        manager.startSession(playerId, "session-a", 0L, 2, 40, 80);
        manager.requestStop(playerId, DynamicCaptureSessionEndReason.RELEASED);

        DynamicCaptureSessionResult result = manager.finishSession(playerId, DynamicCaptureSessionEndReason.INVALIDATED);

        assertEquals(DynamicCaptureSessionEndReason.RELEASED, result.endReason());
        assertFalse(result.shouldCreatePhotograph());
        assertEquals(0, result.frameCount());
    }

    @Test
    void oneFrameSessionFinishesAsProduct() {
        DynamicCaptureSessionManager manager = new DynamicCaptureSessionManager();
        UUID playerId = UUID.randomUUID();

        DynamicCaptureSession session = manager.startSession(playerId, "session-a", 0L, 2, 40, 80);
        session.beginRecording();
        manager.appendFrame(playerId, Frame.EMPTY);
        manager.requestStop(playerId, DynamicCaptureSessionEndReason.RELEASED);

        DynamicCaptureSessionResult result = manager.finishSession(playerId, DynamicCaptureSessionEndReason.INVALIDATED);

        assertTrue(result.shouldCreatePhotograph());
        assertEquals(1, result.frameCount());
    }

    @Test
    void finishedSessionIsRemovedAndPlayerCanStartAgain() {
        DynamicCaptureSessionManager manager = new DynamicCaptureSessionManager();
        UUID playerId = UUID.randomUUID();

        manager.startSession(playerId, "session-a", 0L, 2, 40, 80);
        manager.requestStop(playerId, DynamicCaptureSessionEndReason.RELEASED);
        manager.finishSession(playerId, DynamicCaptureSessionEndReason.INVALIDATED);

        assertTrue(manager.getActiveSession(playerId).isEmpty());
        assertTrue(manager.getActiveSessions().isEmpty());

        DynamicCaptureSession restarted = manager.startSession(playerId, "session-b", 10L, 2, 40, 80);
        assertEquals("session-b", restarted.sessionId());
    }

    @Test
    void clearedSessionsAllowFreshStartAfterAbruptShutdown() {
        DynamicCaptureSessionManager manager = new DynamicCaptureSessionManager();
        UUID playerId = UUID.randomUUID();

        manager.startSession(playerId, "session-a", 0L, 2, 40, 80);
        manager.clear();

        assertTrue(manager.getActiveSession(playerId).isEmpty());
        assertTrue(manager.getActiveSessions().isEmpty());

        DynamicCaptureSession restarted = manager.startSession(playerId, "session-b", 10L, 2, 40, 80);
        assertEquals("session-b", restarted.sessionId());
    }
}
