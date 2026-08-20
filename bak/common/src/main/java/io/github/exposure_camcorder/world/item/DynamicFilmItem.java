package io.github.exposure_camcorder.world.item;

import io.github.exposure_camcorder.Config;
import io.github.exposure_camcorder.ExposureCamcorder;
import io.github.exposure_camcorder.util.DynamicPhotographTiming;
import io.github.exposure_camcorder.util.DynamicPhotographFactory.DynamicPhotographCreationData;
import io.github.exposure_camcorder.util.DynamicPhotographValidation;
import io.github.exposure_camcorder.world.component.DynamicPhotographFrames;
import io.github.exposure_camcorder.world.component.DynamicPhotographSettings;
import io.github.exposure_camcorder.world.component.DynamicPhotographSummary;
import io.github.exposure_camcorder.world.component.DynamicSessionId;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.world.camera.ExposureType;
import io.github.mortuusars.exposure.world.camera.frame.Frame;
import io.github.mortuusars.exposure.world.item.FilmRollItem;
import io.github.mortuusars.exposure.world.item.SensitiveFilmItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.AbstractList;
import java.util.List;
import java.util.RandomAccess;
import java.util.function.IntSupplier;

public class DynamicFilmItem extends FilmRollItem implements SensitiveFilmItem {
    private static final int MAX_EXPOSURE_COMPAT_FRAME_COUNT = 256;
    private final IntSupplier defaultMaxFramesSupplier;
    private final IntSupplier defaultFrameSizeSupplier;

    public DynamicFilmItem(Properties properties) {
        this(ExposureType.COLOR, FilmRollItem.BAR_COLOR, properties,
                Config.Server.DEFAULT_DYNAMIC_FILM_MAX_FRAMES::get,
                io.github.mortuusars.exposure.Config.Server.DEFAULT_FRAME_SIZE::get);
    }

    public DynamicFilmItem(Properties properties, IntSupplier defaultMaxFramesSupplier) {
        this(ExposureType.COLOR, FilmRollItem.BAR_COLOR, properties,
                defaultMaxFramesSupplier,
                io.github.mortuusars.exposure.Config.Server.DEFAULT_FRAME_SIZE::get);
    }

    public DynamicFilmItem(Properties properties, IntSupplier defaultMaxFramesSupplier, IntSupplier defaultFrameSizeSupplier) {
        this(ExposureType.COLOR, FilmRollItem.BAR_COLOR, properties,
                defaultMaxFramesSupplier, defaultFrameSizeSupplier);
    }

    public DynamicFilmItem(ExposureType type, int barColor, Properties properties,
                           IntSupplier defaultMaxFramesSupplier, IntSupplier defaultFrameSizeSupplier) {
        super(type, barColor, properties);
        this.defaultMaxFramesSupplier = defaultMaxFramesSupplier;
        this.defaultFrameSizeSupplier = defaultFrameSizeSupplier;
    }

    public int getMaxFrames(ItemStack stack) {
        return DynamicPhotographValidation.validateMaxFrames(stack.getOrDefault(
                ExposureCamcorder.DataComponents.DYNAMIC_FILM_MAX_FRAMES,
                defaultMaxFramesSupplier.getAsInt()));
    }

    public int getUsedFrames(ItemStack stack) {
        int totalFrames = getMaxFrames(stack);
        int usedFrames = stack.getOrDefault(ExposureCamcorder.DataComponents.DYNAMIC_FILM_USED_FRAMES, 0);
        return Math.max(0, Math.min(totalFrames, usedFrames));
    }

    public int getRemainingFrames(ItemStack stack) {
        if (hasRecordedPhotograph(stack)) {
            return 0;
        }
        return Math.max(0, getMaxFrames(stack) - getUsedFrames(stack));
    }

    public void addUsedFrames(ItemStack stack, int additionalFrames) {
        if (additionalFrames <= 0) {
            return;
        }

        int updatedUsedFrames = Math.min(getMaxFrames(stack), getUsedFrames(stack) + additionalFrames);
        if (updatedUsedFrames <= 0) {
            stack.remove(ExposureCamcorder.DataComponents.DYNAMIC_FILM_USED_FRAMES);
        } else {
            stack.set(ExposureCamcorder.DataComponents.DYNAMIC_FILM_USED_FRAMES, updatedUsedFrames);
        }
    }

    public boolean hasRecordedPhotograph(ItemStack stack) {
        return !stack.getOrDefault(ExposureCamcorder.DataComponents.DYNAMIC_PHOTOGRAPH_FRAMES,
                DynamicPhotographFrames.EMPTY).isEmpty();
    }

    public void setRecordedPhotograph(ItemStack stack, DynamicPhotographCreationData creationData) {
        stack.set(ExposureCamcorder.DataComponents.DYNAMIC_PHOTOGRAPH_FRAMES, creationData.frames());
        stack.set(ExposureCamcorder.DataComponents.DYNAMIC_PHOTOGRAPH_SETTINGS, creationData.settings());
        stack.set(ExposureCamcorder.DataComponents.DYNAMIC_PHOTOGRAPH_SUMMARY, creationData.summary());
        stack.set(ExposureCamcorder.DataComponents.DYNAMIC_PHOTOGRAPH_SESSION_ID, creationData.sessionId());
        stack.set(ExposureCamcorder.DataComponents.DYNAMIC_FILM_USED_FRAMES, creationData.summary().frameCount());
        syncExposureCompatibilityComponents(stack, creationData.summary().frameCount(), getFrameSize(stack));
    }

    @Override
    public int getDefaultMaxFrameCount(ItemStack stack) {
        return defaultMaxFramesSupplier.getAsInt();
    }

    @Override
    public int getMaxFrameCount(ItemStack stack) {
        return getMaxFrames(stack);
    }

    @Override
    public int getDefaultFrameSize(ItemStack stack) {
        return defaultFrameSizeSupplier.getAsInt();
    }

    @Override
    public List<Frame> getStoredFrames(ItemStack stack) {
        if (hasRecordedPhotograph(stack)) {
            return stack.getOrDefault(ExposureCamcorder.DataComponents.DYNAMIC_PHOTOGRAPH_FRAMES,
                    DynamicPhotographFrames.EMPTY).frames();
        }
        return new PlaceholderFramesView(getUsedFrames(stack));
    }

    @Override
    public int getStoredFramesCount(ItemStack stack) {
        if (hasRecordedPhotograph(stack)) {
            DynamicPhotographSummary summary = stack.getOrDefault(ExposureCamcorder.DataComponents.DYNAMIC_PHOTOGRAPH_SUMMARY,
                    new DynamicPhotographSummary(0, 0, 0));
            return summary.frameCount();
        }
        return getUsedFrames(stack);
    }

    @Override
    public boolean canAddFrame(ItemStack stack) {
        return !hasRecordedPhotograph(stack) && super.canAddFrame(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        int actualFrameCount = hasRecordedPhotograph(stack) ? getStoredFramesCount(stack) : 0;
        syncExposureCompatibilityComponents(stack, actualFrameCount, getFrameSize(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
                                TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("item.exposure_camcorder.dynamic_film.tooltip.frame_size", getFrameSize(stack))
                .withStyle(ChatFormatting.GRAY));

        if (hasRecordedPhotograph(stack)) {
            DynamicPhotographSummary summary = stack.getOrDefault(ExposureCamcorder.DataComponents.DYNAMIC_PHOTOGRAPH_SUMMARY,
                    new DynamicPhotographSummary(0, 0, 0));
            DynamicPhotographSettings settings = stack.getOrDefault(ExposureCamcorder.DataComponents.DYNAMIC_PHOTOGRAPH_SETTINGS,
                    DynamicPhotographSettings.DEFAULT);
            tooltipComponents.add(Component.translatable("item.exposure_camcorder.dynamic_film.tooltip.recorded_frames",
                            summary.frameCount())
                    .withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.translatable("item.exposure_camcorder.dynamic_film.tooltip.duration",
                            DynamicPhotographTiming.formatSeconds(
                                    DynamicPhotographTiming.ticksToSeconds(summary.estimatedDurationTicks())))
                    .withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.translatable("item.exposure_camcorder.dynamic_film.tooltip.exposed")
                    .withStyle(ChatFormatting.DARK_GRAY));
            if (tooltipFlag.isAdvanced()) {
                DynamicSessionId sessionId = stack.getOrDefault(ExposureCamcorder.DataComponents.DYNAMIC_PHOTOGRAPH_SESSION_ID,
                        DynamicSessionId.EMPTY);
                if (!sessionId.value().isEmpty()) {
                    tooltipComponents.add(Component.translatable("item.exposure_camcorder.dynamic_photograph.tooltip.session",
                                    sessionId.value())
                            .withStyle(ChatFormatting.DARK_GRAY));
                }
            }
            return;
        }

        int maxFrames = getMaxFrames(stack);
        int remainingFrames = getRemainingFrames(stack);
        int captureInterval = Config.Server.DEFAULT_CAPTURE_INTERVAL_TICKS.get();
        double durationSeconds = DynamicPhotographTiming.estimateDurationSeconds(remainingFrames, captureInterval);

        tooltipComponents.add(Component.translatable("item.exposure_camcorder.dynamic_film.tooltip.max_frames", maxFrames)
                .withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("item.exposure_camcorder.dynamic_film.tooltip.remaining_frames", remainingFrames)
                .withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("item.exposure_camcorder.dynamic_film.tooltip.duration",
                        DynamicPhotographTiming.formatSeconds(durationSeconds))
                .withStyle(ChatFormatting.GRAY));
    }

    private static final class PlaceholderFramesView extends AbstractList<Frame> implements RandomAccess {
        private final int size;

        private PlaceholderFramesView(int size) {
            this.size = size;
        }

        @Override
        public Frame get(int index) {
            if (index < 0 || index >= size) {
                throw new IndexOutOfBoundsException(index);
            }
            return Frame.EMPTY;
        }

        @Override
        public int size() {
            return size;
        }
    }

    public static void syncExposureCompatibilityComponents(ItemStack stack, int actualFrameCount, int frameSize) {
        if (actualFrameCount > 0) {
            stack.set(Exposure.DataComponents.FILM_FRAME_COUNT,
                    Math.min(actualFrameCount, MAX_EXPOSURE_COMPAT_FRAME_COUNT));
        } else {
            stack.remove(Exposure.DataComponents.FILM_FRAME_COUNT);
        }

        if (frameSize > 0) {
            stack.set(Exposure.DataComponents.FILM_FRAME_SIZE, frameSize);
        } else {
            stack.remove(Exposure.DataComponents.FILM_FRAME_SIZE);
        }
    }
}
