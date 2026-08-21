package io.github.exposure_camcorder.client.gui.component;

import io.github.exposure_camcorder.client.playback.DynamicPlaybackSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DynamicPlaybackControlsTest {
    @Test
    void speedUpAndSlowDownAdjustSessionSpeedLevels() {
        DynamicPlaybackControls controls = new DynamicPlaybackControls(2);
        DynamicPlaybackSession session = new DynamicPlaybackSession(4, 2, true);

        assertTrue(controls.speedUp(session));
        assertEquals(1.25d, session.speedMultiplier());

        assertTrue(controls.slowDown(session));
        assertEquals(1.0d, session.speedMultiplier());
    }

    @Test
    void speedLabelUsesDiscreteMultiplierLabels() {
        DynamicPlaybackControls controls = new DynamicPlaybackControls(4);
        DynamicPlaybackSession session = new DynamicPlaybackSession(4, 2, true);
        session.setSpeedMultiplier(1.5d);

        assertEquals(1.5d, controls.speedMultiplier(session));
        assertEquals("x1.5", controls.speedLabel(session));
    }

    @Test
    void controlsRespectDiscretePlaybackRateBounds() {
        DynamicPlaybackControls controls = new DynamicPlaybackControls(2);
        DynamicPlaybackSession fastest = new DynamicPlaybackSession(4, 2, true);
        fastest.setSpeedMultiplier(2.0d);
        DynamicPlaybackSession slowest = new DynamicPlaybackSession(4, 2, true);
        slowest.setSpeedMultiplier(0.1d);

        assertFalse(controls.speedUp(fastest));
        assertEquals(2.0d, fastest.speedMultiplier());

        assertFalse(controls.slowDown(slowest));
        assertEquals(0.1d, slowest.speedMultiplier());
    }
}
