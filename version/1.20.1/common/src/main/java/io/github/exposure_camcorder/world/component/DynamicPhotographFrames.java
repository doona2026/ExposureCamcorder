package io.github.exposure_camcorder.world.component;

import com.mojang.serialization.Codec;
import io.github.mortuusars.exposure.world.camera.frame.Frame;

import java.util.List;
import java.util.Optional;

public record DynamicPhotographFrames(List<Frame> frames) {
    public static final int MAX_FRAMES = 600;
    public static final DynamicPhotographFrames EMPTY = new DynamicPhotographFrames(List.of());

    public static final Codec<DynamicPhotographFrames> CODEC = Codec.list(Frame.CODEC)
            .xmap(DynamicPhotographFrames::new, DynamicPhotographFrames::frames);

    public DynamicPhotographFrames {
        if (frames.size() > MAX_FRAMES) {
            throw new IllegalArgumentException("Too many frames: " + frames.size());
        }
        frames = List.copyOf(frames);
    }

    public int size() {
        return frames.size();
    }

    public boolean isEmpty() {
        return frames.isEmpty();
    }

    public Optional<Frame> getFrame(int index) {
        return index >= 0 && index < frames.size() ? Optional.of(frames.get(index)) : Optional.empty();
    }

    public Optional<Frame> getCoverFrame(int preferredIndex) {
        if (frames.isEmpty()) {
            return Optional.empty();
        }

        if (preferredIndex >= 0 && preferredIndex < frames.size()) {
            Frame preferred = frames.get(preferredIndex);
            if (!preferred.identifier().isEmpty()) {
                return Optional.of(preferred);
            }
        }

        return frames.stream().filter(frame -> !frame.identifier().isEmpty()).findFirst();
    }
}
