package io.github.exposure_camcorder.world.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record DynamicPhotographSummary(int frameCount, int estimatedDurationTicks, int coverFrameIndex) {
    public static final Codec<DynamicPhotographSummary> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("frame_count").forGetter(DynamicPhotographSummary::frameCount),
            Codec.INT.fieldOf("estimated_duration_ticks").forGetter(DynamicPhotographSummary::estimatedDurationTicks),
            Codec.INT.optionalFieldOf("cover_frame_index", 0).forGetter(DynamicPhotographSummary::coverFrameIndex)
    ).apply(instance, DynamicPhotographSummary::new));

    public DynamicPhotographSummary {
        if (frameCount < 0) {
            throw new IllegalArgumentException("frameCount cannot be negative.");
        }
        if (estimatedDurationTicks < 0) {
            throw new IllegalArgumentException("estimatedDurationTicks cannot be negative.");
        }
        if (coverFrameIndex < 0) {
            throw new IllegalArgumentException("coverFrameIndex cannot be negative.");
        }
    }
}
