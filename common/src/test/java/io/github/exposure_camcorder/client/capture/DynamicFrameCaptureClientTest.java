package io.github.exposure_camcorder.client.capture;

import io.github.exposure_camcorder.network.packet.s2c.DynamicCaptureStartS2CP;
import io.github.exposure_camcorder.network.packet.s2c.DynamicCaptureStateS2CP;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DynamicFrameCaptureClientTest {
    @AfterEach
    void tearDown() {
        DynamicFrameCaptureClient.reset();
    }

    @Test
    void ignoresStateUpdatesFromDifferentSession() {
        DynamicFrameCaptureClient.startSession(new DynamicCaptureStartS2CP("session-a", 2, 40, 160));

        DynamicFrameCaptureClient.updateState(new DynamicCaptureStateS2CP("session-b", 12, 28, 120, false));

        DynamicRecordingClientState state = DynamicFrameCaptureClient.getRecordingState();
        assertEquals("session-a", state.sessionId());
        assertEquals(0, state.recordedFrames());
        assertEquals(40, state.maxFrames());
        assertEquals(40, state.remainingFrames());
        assertEquals(160, state.remainingDurationTicks());
    }
}
