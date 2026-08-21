package io.github.exposure_camcorder.world.component;

import io.github.mortuusars.exposure.world.camera.frame.Frame;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DynamicPhotographFramesTest {
    @Test
    void supportsSixHundredFrameProducts() {
        assertDoesNotThrow(() -> new DynamicPhotographFrames(Collections.nCopies(600, Frame.EMPTY)));
    }

    @Test
    void rejectsProductsAboveTheSupportedLimit() {
        assertThrows(IllegalArgumentException.class,
                () -> new DynamicPhotographFrames(Collections.nCopies(601, Frame.EMPTY)));
    }
}
