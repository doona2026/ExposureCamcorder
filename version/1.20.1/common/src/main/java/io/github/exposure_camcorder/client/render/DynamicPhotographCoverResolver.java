package io.github.exposure_camcorder.client.render;

import io.github.exposure_camcorder.ExposureCamcorder;
import io.github.exposure_camcorder.world.component.DynamicPhotographFrames;
import io.github.exposure_camcorder.world.component.DynamicPhotographSummary;
import io.github.mortuusars.exposure.world.camera.frame.Frame;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class DynamicPhotographCoverResolver {
    private final DynamicPhotographFrameResolver frameResolver;

    public DynamicPhotographCoverResolver() {
        this(new DynamicPhotographFrameResolver());
    }

    public DynamicPhotographCoverResolver(DynamicPhotographFrameResolver frameResolver) {
        this.frameResolver = frameResolver;
    }

    public Optional<Frame> resolve(ItemStack stack) {
        DynamicPhotographFrames frames = io.github.exposure_camcorder.util.DynamicItemStackData.getDynamicPhotographFrames(stack, DynamicPhotographFrames.EMPTY);
        DynamicPhotographSummary summary = io.github.exposure_camcorder.util.DynamicItemStackData.getDynamicPhotographSummary(stack, new DynamicPhotographSummary(0, 0, 0));
        return resolve(frames, summary.coverFrameIndex());
    }

    public Optional<Frame> resolve(DynamicPhotographFrames frames, int preferredIndex) {
        return frameResolver.resolve(frames, preferredIndex);
    }
}
