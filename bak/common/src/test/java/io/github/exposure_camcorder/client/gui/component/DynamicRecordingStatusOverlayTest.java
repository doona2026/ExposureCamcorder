package io.github.exposure_camcorder.client.gui.component;

import io.github.exposure_camcorder.client.capture.DynamicRecordingClientState;
import io.github.exposure_camcorder.world.session.DynamicCaptureSession;
import io.github.mortuusars.exposure.world.camera.frame.Frame;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicRecordingStatusOverlayTest {
    private final DynamicRecordingStatusOverlay overlay = new DynamicRecordingStatusOverlay();

    @Test
    void derivesRemainingValuesFromSession() {
        DynamicCaptureSession session = new DynamicCaptureSession("session", UUID.randomUUID(), 100L, 4, 12, 80);
        session.beginRecording();
        session.appendFrame(Frame.EMPTY);
        session.appendFrame(Frame.EMPTY);

        assertTrue(overlay.shouldRender(session));
        assertEquals(10, overlay.remainingCapacity(session));
        assertEquals(72, overlay.remainingDurationTicks(session, 108L));
        assertEquals(3.6d, overlay.remainingDurationSeconds(session, 108L), 0.0001d);
    }

    @Test
    void remainingDurationClampsAtZero() {
        DynamicCaptureSession session = new DynamicCaptureSession("session", UUID.randomUUID(), 100L, 4, 12, 40);

        assertEquals(0, overlay.remainingDurationTicks(session, 200L));
        assertEquals(0.0d, overlay.remainingDurationSeconds(session, 200L), 0.0001d);
    }

    @Test
    void derivesProgressFromClientState() {
        DynamicRecordingClientState state = new DynamicRecordingClientState("session", 3, 12, 9, 40);

        assertTrue(overlay.shouldRender(state));
        assertEquals(9, overlay.remainingCapacity(state));
        assertEquals(0.25f, overlay.progressFraction(state), 0.0001f);
    }
}
