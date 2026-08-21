package io.github.exposure_camcorder.world.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record DynamicPhotographSettings(int captureIntervalTicks,
                                        int defaultPlaybackTicksPerFrame,
                                        boolean loop,
                                        int coverFrameIndex) {
    public static final DynamicPhotographSettings DEFAULT = new DynamicPhotographSettings(2, 2, true, 0);

    public static final Codec<DynamicPhotographSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("capture_interval_ticks").forGetter(DynamicPhotographSettings::captureIntervalTicks),
            Codec.INT.fieldOf("default_playback_ticks_per_frame").forGetter(DynamicPhotographSettings::defaultPlaybackTicksPerFrame),
            Codec.BOOL.optionalFieldOf("loop", true).forGetter(DynamicPhotographSettings::loop),
            Codec.INT.optionalFieldOf("cover_frame_index", 0).forGetter(DynamicPhotographSettings::coverFrameIndex)
    ).apply(instance, DynamicPhotographSettings::new));

    public DynamicPhotographSettings {
        if (captureIntervalTicks <= 0) {
            throw new IllegalArgumentException("captureIntervalTicks must be positive.");
        }
        if (defaultPlaybackTicksPerFrame <= 0) {
            throw new IllegalArgumentException("defaultPlaybackTicksPerFrame must be positive.");
        }
        if (coverFrameIndex < 0) {
            throw new IllegalArgumentException("coverFrameIndex cannot be negative.");
        }
    }
}
