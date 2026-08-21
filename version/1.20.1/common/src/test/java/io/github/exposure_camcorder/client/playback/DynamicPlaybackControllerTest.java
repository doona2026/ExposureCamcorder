package io.github.exposure_camcorder.client.playback;

import io.github.exposure_camcorder.world.component.DynamicPhotographSettings;
import io.github.exposure_camcorder.world.component.DynamicPhotographSummary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DynamicPlaybackControllerTest {
    private final DynamicPlaybackController controller = new DynamicPlaybackController();

    @Test
    void createSessionStartsAtFirstFrame() {
        DynamicPlaybackSession session = controller.createSession(
                new DynamicPhotographSummary(3, 6, 0),
                new DynamicPhotographSettings(2, 2, true, 0));

        assertEquals(0, session.currentFrameIndex());
        assertEquals(2, session.defaultTicksPerFrame());
        assertFalse(session.paused());
    }

    @Test
    void tickAdvancesWhenIntervalElapses() {
        DynamicPlaybackSession session = new DynamicPlaybackSession(3, 2, true);

        assertFalse(controller.tick(session));
        assertEquals(0, session.currentFrameIndex());

        assertTrue(controller.tick(session));
        assertEquals(1, session.currentFrameIndex());
    }

    @Test
    void nonLoopingSessionPausesAtLastFrame() {
        DynamicPlaybackSession session = new DynamicPlaybackSession(2, 1, false);

        assertTrue(controller.tick(session));
        assertEquals(1, session.currentFrameIndex());
        assertFalse(session.paused());

        assertFalse(controller.tick(session));
        assertEquals(0, session.currentFrameIndex());
        assertTrue(session.paused());
    }

    @Test
    void loopingSessionAlsoResetsToFirstFrameAndPauses() {
        DynamicPlaybackSession session = new DynamicPlaybackSession(2, 1, true);

        assertTrue(controller.tick(session));
        assertEquals(1, session.currentFrameIndex());

        assertFalse(controller.tick(session));
        assertEquals(0, session.currentFrameIndex());
        assertTrue(session.paused());
    }

    @Test
    void togglePausedStopsPlayback() {
        DynamicPlaybackSession session = new DynamicPlaybackSession(3, 1, true);
        session.pause();

        assertFalse(controller.tick(session));
        assertEquals(0, session.currentFrameIndex());

        session.resume();
        assertTrue(controller.tick(session));
        assertEquals(1, session.currentFrameIndex());
    }

    @Test
    void tickRespectsSpeedChanges() {
        DynamicPlaybackSession session = new DynamicPlaybackSession(3, 2, true);

        assertFalse(controller.tick(session));
        assertEquals(0, session.currentFrameIndex());

        session.setSpeedMultiplier(2.0d);
        assertTrue(controller.tick(session));
        assertEquals(1, session.currentFrameIndex());
    }

    @Test
    void slowPlaybackRequiresMultipleTicksBeforeAdvancing() {
        DynamicPlaybackSession session = new DynamicPlaybackSession(3, 2, true);
        session.setSpeedMultiplier(0.25d);

        for (int tick = 0; tick < 7; tick++) {
            assertFalse(controller.tick(session));
            assertEquals(0, session.currentFrameIndex());
        }

        assertTrue(controller.tick(session));
        assertEquals(1, session.currentFrameIndex());
    }

    @Test
    void fastPlaybackStillResetsProgressAfterEachFrameAdvance() {
        DynamicPlaybackSession session = new DynamicPlaybackSession(5, 2, true);
        session.setSpeedMultiplier(3.0d);

        assertTrue(controller.tick(session));
        assertEquals(1, session.currentFrameIndex());
        assertEquals(0.0d, session.tickProgress(), 0.000001d);

        assertTrue(controller.tick(session));
        assertEquals(2, session.currentFrameIndex());
        assertEquals(0.0d, session.tickProgress(), 0.000001d);
    }
}
