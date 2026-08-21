package io.github.exposure_camcorder.client.gui.component;

import io.github.exposure_camcorder.client.capture.DynamicRecordingClientState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicRecordingStatusOverlayTest {
    private final DynamicRecordingStatusOverlay overlay = new DynamicRecordingStatusOverlay();

    @Test
    void derivesProgressFromClientState() {
        DynamicRecordingClientState state = new DynamicRecordingClientState("session", 3, 12, 9);

        assertTrue(overlay.shouldRender(state));
        assertEquals(9, overlay.remainingCapacity(state));
        assertEquals(0.25f, overlay.progressFraction(state), 0.0001f);
    }
}
