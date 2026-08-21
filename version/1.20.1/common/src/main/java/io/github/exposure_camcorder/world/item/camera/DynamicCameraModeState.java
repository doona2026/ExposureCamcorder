package io.github.exposure_camcorder.world.item.camera;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.exposure_camcorder.world.item.DynamicMode;

public record DynamicCameraModeState(DynamicMode mode,
                                     int captureIntervalTicks,
                                     int defaultPlaybackTicksPerFrame,
                                     boolean filmLoaded) {
    public static final DynamicCameraModeState DEFAULT =
            new DynamicCameraModeState(DynamicMode.REGULAR, 2, 2, false);

    public static final Codec<DynamicCameraModeState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DynamicMode.CODEC.fieldOf("mode").forGetter(DynamicCameraModeState::mode),
            Codec.INT.fieldOf("capture_interval_ticks").forGetter(DynamicCameraModeState::captureIntervalTicks),
            Codec.INT.fieldOf("default_playback_ticks_per_frame").forGetter(DynamicCameraModeState::defaultPlaybackTicksPerFrame),
            Codec.BOOL.optionalFieldOf("film_loaded", false).forGetter(DynamicCameraModeState::filmLoaded)
    ).apply(instance, DynamicCameraModeState::new));

    public DynamicCameraModeState {
        if (mode == null) {
            throw new IllegalArgumentException("mode must be present.");
        }
        if (captureIntervalTicks <= 0) {
            throw new IllegalArgumentException("captureIntervalTicks must be positive.");
        }
        if (defaultPlaybackTicksPerFrame <= 0) {
            throw new IllegalArgumentException("defaultPlaybackTicksPerFrame must be positive.");
        }
    }

    public boolean isDynamic() {
        return mode == DynamicMode.DYNAMIC;
    }

    public boolean canStartRecording() {
        return isDynamic() && filmLoaded;
    }

    public DynamicCameraModeState withMode(DynamicMode newMode) {
        return new DynamicCameraModeState(newMode, captureIntervalTicks, defaultPlaybackTicksPerFrame, filmLoaded);
    }

    public DynamicCameraModeState withFilmLoaded(boolean loaded) {
        return new DynamicCameraModeState(mode, captureIntervalTicks, defaultPlaybackTicksPerFrame, loaded);
    }
}
