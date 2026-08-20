package io.github.exposure_camcorder.world.item;

import io.github.exposure_camcorder.util.DynamicPhotographFactory;
import io.github.exposure_camcorder.util.DynamicPhotographTiming;
import io.github.exposure_camcorder.world.component.DynamicPhotographSettings;
import io.github.exposure_camcorder.world.component.DynamicPhotographSummary;
import io.github.exposure_camcorder.world.component.DynamicSessionId;
import io.github.mortuusars.exposure.world.camera.frame.Frame;
import io.github.mortuusars.exposure.world.level.storage.ExposureIdentifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DynamicPhotographFactoryTest {
    @Test
    void createWritesFramesSettingsSummaryAndSessionId() {
        Frame first = frame("first");
        Frame cover = frame("cover");
        DynamicPhotographSettings settings = new DynamicPhotographSettings(3, 4, true, 1);

        DynamicPhotographFactory.DynamicPhotographCreationData data =
                DynamicPhotographFactory.buildCreationData(List.of(first, cover), settings, "session-123");

        assertEquals(2, data.frames().size());
        DynamicPhotographSummary summary = data.summary();
        assertEquals(2, summary.frameCount());
        assertEquals(DynamicPhotographTiming.estimateDurationTicks(2, 3), summary.estimatedDurationTicks());
        assertEquals(1, summary.coverFrameIndex());
        assertEquals(settings, data.settings());
        assertEquals(new DynamicSessionId("session-123"), data.sessionId());
    }

    @Test
    void createRejectsEmptyFrameList() {
        DynamicPhotographSettings settings = new DynamicPhotographSettings(2, 2, true, 0);

        assertThrows(IllegalArgumentException.class,
                () -> DynamicPhotographFactory.buildCreationData(List.of(), settings, "session-123"));
    }

    private static Frame frame(String id) {
        return Frame.EMPTY.toMutable()
                .setIdentifier(ExposureIdentifier.id(id))
                .toImmutable();
    }
}
