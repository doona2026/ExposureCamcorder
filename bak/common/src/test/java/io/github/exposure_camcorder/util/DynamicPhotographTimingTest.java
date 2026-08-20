package io.github.exposure_camcorder.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DynamicPhotographTimingTest {
    @Test
    void ticksConvertToSeconds() {
        assertEquals(0.5d, DynamicPhotographTiming.ticksToSeconds(10), 0.0001d);
        assertEquals(4.0d, DynamicPhotographTiming.ticksToSeconds(80), 0.0001d);
    }

    @Test
    void negativeTicksAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> DynamicPhotographTiming.ticksToSeconds(-1));
    }

    @Test
    void maxFramesAreDerivedFromDurationAndInterval() {
        assertEquals(40, DynamicPhotographTiming.calculateMaxFrames(80, 2));
        assertEquals(20, DynamicPhotographTiming.calculateMaxFrames(80, 4));
    }

    @Test
    void estimatedDurationUsesFrameCountAndInterval() {
        assertEquals(80, DynamicPhotographTiming.estimateDurationTicks(40, 2));
        assertEquals(4.0d, DynamicPhotographTiming.estimateDurationSeconds(40, 2), 0.0001d);
    }
}
