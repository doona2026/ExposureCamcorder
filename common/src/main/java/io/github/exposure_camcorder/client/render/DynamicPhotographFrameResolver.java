package io.github.exposure_camcorder.client.render;

import io.github.exposure_camcorder.world.component.DynamicPhotographFrames;
import io.github.mortuusars.exposure.world.camera.frame.Frame;

import java.util.Optional;

public class DynamicPhotographFrameResolver {
    public Optional<Frame> resolve(DynamicPhotographFrames frames, int preferredIndex) {
        if (frames.isEmpty()) {
            return Optional.empty();
        }

        int sanitizedIndex = Math.max(0, preferredIndex);

        for (int index = sanitizedIndex; index < frames.size(); index++) {
            Optional<Frame> candidate = frames.getFrame(index);
            if (candidate.isPresent() && !candidate.get().identifier().isEmpty()) {
                return candidate;
            }
        }

        for (int index = 0; index < sanitizedIndex && index < frames.size(); index++) {
            Optional<Frame> candidate = frames.getFrame(index);
            if (candidate.isPresent() && !candidate.get().identifier().isEmpty()) {
                return candidate;
            }
        }

        return Optional.empty();
    }
}
