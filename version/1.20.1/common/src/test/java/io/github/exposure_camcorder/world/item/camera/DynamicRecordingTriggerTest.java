package io.github.exposure_camcorder.world.item.camera;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicRecordingTriggerTest {
    @Test
    void durationBudgetScalesWithFilmCapacityAndFrameArea() {
        DynamicRecordingTrigger trigger = new DynamicRecordingTrigger();

        assertEquals(320, trigger.calculateDurationBudgetTicks(40, 2, 320, 320, 80));
        assertEquals(1280, trigger.calculateDurationBudgetTicks(40, 2, 640, 320, 80));
        assertEquals(2880, trigger.calculateDurationBudgetTicks(40, 2, 960, 320, 80));
        assertTrue(trigger.calculateDurationBudgetTicks(1, 2, 320, 320, 80) >= 80);
    }

    @Test
    void longAndHighResolutionFilmBudgetsRemainMonotonic() {
        DynamicRecordingTrigger trigger = new DynamicRecordingTrigger();

        int standard80 = trigger.calculateDurationBudgetTicks(80, 2, 320, 320, 80);
        int standard160 = trigger.calculateDurationBudgetTicks(160, 2, 320, 320, 80);
        int standard320 = trigger.calculateDurationBudgetTicks(320, 2, 320, 320, 80);
        int standard600 = trigger.calculateDurationBudgetTicks(600, 2, 320, 320, 80);
        int hires80 = trigger.calculateDurationBudgetTicks(80, 2, 640, 320, 80);
        int hires160 = trigger.calculateDurationBudgetTicks(160, 2, 640, 320, 80);
        int hires320 = trigger.calculateDurationBudgetTicks(320, 2, 640, 320, 80);
        int hires600 = trigger.calculateDurationBudgetTicks(600, 2, 640, 320, 80);

        assertTrue(standard80 < standard160);
        assertTrue(standard160 < standard320);
        assertTrue(standard320 < standard600);
        assertTrue(hires80 < hires160);
        assertTrue(hires160 < hires320);
        assertTrue(hires320 < hires600);
        assertTrue(hires80 > standard80);
        assertTrue(hires160 > standard160);
        assertTrue(hires320 > standard320);
        assertTrue(hires600 > standard600);
    }
}
