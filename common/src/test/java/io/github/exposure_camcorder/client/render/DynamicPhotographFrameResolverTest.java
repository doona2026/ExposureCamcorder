package io.github.exposure_camcorder.client.render;

import io.github.exposure_camcorder.world.component.DynamicPhotographFrames;
import io.github.mortuusars.exposure.world.camera.frame.Frame;
import io.github.mortuusars.exposure.world.level.storage.ExposureIdentifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DynamicPhotographFrameResolverTest {
    private final DynamicPhotographFrameResolver resolver = new DynamicPhotographFrameResolver();

    @Test
    void resolvesExactFrameWhenPresent() {
        Frame target = frame("target");
        DynamicPhotographFrames frames = new DynamicPhotographFrames(List.of(frame("a"), target, frame("b")));

        assertEquals(target, resolver.resolve(frames, 1).orElseThrow());
    }

    @Test
    void fallsForwardToNextAvailableFrame() {
        Frame fallback = frame("fallback");
        DynamicPhotographFrames frames = new DynamicPhotographFrames(List.of(Frame.EMPTY, Frame.EMPTY, fallback));

        assertEquals(fallback, resolver.resolve(frames, 0).orElseThrow());
    }

    @Test
    void wrapsToEarlierAvailableFrameWhenNeeded() {
        Frame wrap = frame("wrap");
        DynamicPhotographFrames frames = new DynamicPhotographFrames(List.of(wrap, Frame.EMPTY, Frame.EMPTY));

        assertEquals(wrap, resolver.resolve(frames, 2).orElseThrow());
    }

    @Test
    void returnsEmptyWhenAllFramesAreMissing() {
        DynamicPhotographFrames frames = new DynamicPhotographFrames(List.of(Frame.EMPTY, Frame.EMPTY));

        assertTrue(resolver.resolve(frames, 0).isEmpty());
    }

    private static Frame frame(String id) {
        return Frame.EMPTY.toMutable()
                .setIdentifier(ExposureIdentifier.id(id))
                .toImmutable();
    }
}
