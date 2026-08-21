package io.github.exposure_camcorder.compatibility.exposure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ExposureAccessTest {
    @Test
    void retryExposureIdsAreUniquePerAttempt() {
        String baseId = ExposureAccess.createExposureId("session-a", 12);
        String retry1Id = ExposureAccess.createExposureId("session-a", 12, 1);
        String retry2Id = ExposureAccess.createExposureId("session-a", 12, 2);

        assertEquals(baseId, ExposureAccess.createExposureId("session-a", 12, 0));
        assertNotEquals(baseId, retry1Id);
        assertNotEquals(retry1Id, retry2Id);
    }
}
