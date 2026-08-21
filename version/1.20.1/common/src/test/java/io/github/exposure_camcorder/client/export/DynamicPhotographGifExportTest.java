package io.github.exposure_camcorder.client.export;

import io.github.exposure_camcorder.world.component.DynamicPhotographFrames;
import io.github.exposure_camcorder.world.component.DynamicSessionId;
import io.github.mortuusars.exposure.util.ExtraData;
import io.github.mortuusars.exposure.world.camera.ExposureType;
import io.github.mortuusars.exposure.world.camera.frame.Frame;
import io.github.mortuusars.exposure.world.camera.frame.Photographer;
import io.github.mortuusars.exposure.world.level.storage.ExposureIdentifier;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DynamicPhotographGifExportTest {
    @Test
    void collectExposureIdsSkipsEmptyAndTextureFrames() {
        DynamicPhotographFrames frames = new DynamicPhotographFrames(List.of(
                Frame.EMPTY,
                frameWithIdentifier(ExposureIdentifier.id("frame_a")),
                frameWithIdentifier(ExposureIdentifier.texture(new ResourceLocation("exposure", "test"))),
                frameWithIdentifier(ExposureIdentifier.id("frame_b"))
        ));

        assertEquals(List.of("frame_a", "frame_b"), DynamicPhotographGifExport.collectExposureIds(frames));
    }

    @Test
    void resolveFrameDelayUsesPlaybackTicks() {
        assertEquals(5, DynamicPhotographGifExport.resolveFrameDelayCentiseconds(1.0d));
        assertEquals(8, DynamicPhotographGifExport.resolveFrameDelayCentiseconds(1.5d));
        assertEquals(10, DynamicPhotographGifExport.resolveFrameDelayCentiseconds(2.0d));
    }

    @Test
    void resolveFilenamePrefersSessionId() {
        assertEquals("session-123_dynamic", DynamicPhotographGifExport.resolveFilename(
                new DynamicSessionId("session-123"), List.of("frame_a")));
    }

    @Test
    void resolveFilenameFallsBackToFirstExposureId() {
        assertEquals("frame_a_dynamic", DynamicPhotographGifExport.resolveFilename(
                DynamicSessionId.EMPTY, List.of("frame_a", "frame_b")));
    }

    private Frame frameWithIdentifier(ExposureIdentifier identifier) {
        return new Frame(identifier, ExposureType.COLOR, Photographer.EMPTY, List.of(), ExtraData.EMPTY);
    }
}
