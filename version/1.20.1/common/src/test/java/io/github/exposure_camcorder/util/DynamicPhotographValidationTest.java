package io.github.exposure_camcorder.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicPhotographValidationTest {
    @Test
    void zeroFramesCannotCreateAProduct() {
        assertFalse(DynamicPhotographValidation.canCreatePhotograph(0));
        assertThrows(IllegalArgumentException.class, () -> DynamicPhotographValidation.validateFrameCountForProduct(0));
    }

    @Test
    void oneFrameIsEnoughToCreateAProduct() {
        assertTrue(DynamicPhotographValidation.canCreatePhotograph(1));
        assertDoesNotThrow(() -> DynamicPhotographValidation.validateFrameCountForProduct(1));
    }

    @Test
    void nonPositiveCaptureIntervalIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> DynamicPhotographValidation.validateCaptureInterval(0));
        assertThrows(IllegalArgumentException.class, () -> DynamicPhotographValidation.validateCaptureInterval(-2));
    }

    @Test
    void positiveMaxFramesAreRequired() {
        assertThrows(IllegalArgumentException.class, () -> DynamicPhotographValidation.validateMaxFrames(0));
        assertDoesNotThrow(() -> DynamicPhotographValidation.validateMaxFrames(12));
        assertDoesNotThrow(() -> DynamicPhotographValidation.validateMaxFrames(600));
        assertThrows(IllegalArgumentException.class, () -> DynamicPhotographValidation.validateMaxFrames(601));
    }
}
