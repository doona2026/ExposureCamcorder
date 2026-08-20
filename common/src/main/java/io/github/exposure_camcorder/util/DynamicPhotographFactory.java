package io.github.exposure_camcorder.util;

import io.github.exposure_camcorder.ExposureCamcorder;
import io.github.exposure_camcorder.world.component.DynamicPhotographFrames;
import io.github.exposure_camcorder.world.component.DynamicPhotographSettings;
import io.github.exposure_camcorder.world.component.DynamicPhotographSummary;
import io.github.exposure_camcorder.world.component.DynamicSessionId;
import io.github.mortuusars.exposure.world.camera.frame.Frame;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

public final class DynamicPhotographFactory {
    public record DynamicPhotographCreationData(DynamicPhotographFrames frames,
                                                DynamicPhotographSettings settings,
                                                DynamicPhotographSummary summary,
                                                DynamicSessionId sessionId) {
    }

    private DynamicPhotographFactory() {
    }

    public static ItemStack create(List<Frame> frames, DynamicPhotographSettings settings, String sessionId) {
        return create(frames, settings, sessionId,
                () -> new ItemStack(ExposureCamcorder.Items.DYNAMIC_PHOTOGRAPH.get()));
    }

    public static ItemStack create(DynamicPhotographFrames frames, DynamicPhotographSettings settings, String sessionId) {
        return create(frames, settings, sessionId,
                () -> new ItemStack(ExposureCamcorder.Items.DYNAMIC_PHOTOGRAPH.get()));
    }

    public static ItemStack create(List<Frame> frames, DynamicPhotographSettings settings, String sessionId,
                                   Supplier<ItemStack> stackFactory) {
        DynamicPhotographCreationData creationData = buildCreationData(frames, settings, sessionId);
        return create(creationData, stackFactory);
    }

    public static ItemStack create(DynamicPhotographFrames frames, DynamicPhotographSettings settings, String sessionId,
                                   Supplier<ItemStack> stackFactory) {
        DynamicPhotographCreationData creationData = buildCreationData(frames, settings, sessionId);
        return create(creationData, stackFactory);
    }

    private static ItemStack create(DynamicPhotographCreationData creationData, Supplier<ItemStack> stackFactory) {
        ItemStack stack = stackFactory.get();
        stack.set(ExposureCamcorder.DataComponents.DYNAMIC_PHOTOGRAPH_FRAMES, creationData.frames());
        stack.set(ExposureCamcorder.DataComponents.DYNAMIC_PHOTOGRAPH_SETTINGS, creationData.settings());
        stack.set(ExposureCamcorder.DataComponents.DYNAMIC_PHOTOGRAPH_SUMMARY, creationData.summary());
        stack.set(ExposureCamcorder.DataComponents.DYNAMIC_PHOTOGRAPH_SESSION_ID, creationData.sessionId());
        return stack;
    }

    public static DynamicPhotographCreationData buildCreationData(List<Frame> frames,
                                                                  DynamicPhotographSettings settings,
                                                                  String sessionId) {
        DynamicPhotographValidation.validateFrameCountForProduct(frames.size());
        return buildCreationData(new DynamicPhotographFrames(frames), settings, sessionId);
    }

    public static DynamicPhotographCreationData buildCreationData(DynamicPhotographFrames frames,
                                                                  DynamicPhotographSettings settings,
                                                                  String sessionId) {
        DynamicPhotographValidation.validateFrameCountForProduct(frames.size());
        int estimatedDurationTicks = DynamicPhotographTiming.estimateDurationTicks(frames.size(), settings.captureIntervalTicks());
        DynamicPhotographSummary summary = new DynamicPhotographSummary(frames.size(), estimatedDurationTicks,
                settings.coverFrameIndex());
        return new DynamicPhotographCreationData(frames, settings, summary, new DynamicSessionId(sessionId));
    }
}
