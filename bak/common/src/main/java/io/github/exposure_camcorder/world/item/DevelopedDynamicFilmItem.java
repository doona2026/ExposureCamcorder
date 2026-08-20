package io.github.exposure_camcorder.world.item;

import io.github.exposure_camcorder.ExposureCamcorder;
import io.github.exposure_camcorder.util.DynamicPhotographTiming;
import io.github.exposure_camcorder.world.component.DynamicPhotographFrames;
import io.github.exposure_camcorder.world.component.DynamicPhotographSettings;
import io.github.exposure_camcorder.world.component.DynamicPhotographSummary;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.world.camera.ExposureType;
import io.github.mortuusars.exposure.world.camera.frame.Frame;
import io.github.mortuusars.exposure.world.item.DevelopedFilmItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class DevelopedDynamicFilmItem extends DevelopedFilmItem {
    public DevelopedDynamicFilmItem(ExposureType type, Properties properties) {
        super(type, properties);
    }

    @Override
    public List<Frame> getStoredFrames(ItemStack stack) {
        return stack.getOrDefault(ExposureCamcorder.DataComponents.DYNAMIC_PHOTOGRAPH_FRAMES,
                DynamicPhotographFrames.EMPTY).frames();
    }

    @Override
    public int getStoredFramesCount(ItemStack stack) {
        DynamicPhotographSummary summary = stack.getOrDefault(ExposureCamcorder.DataComponents.DYNAMIC_PHOTOGRAPH_SUMMARY,
                new DynamicPhotographSummary(0, 0, 0));
        return summary.frameCount();
    }

    @Override
    public int getFrameSize(ItemStack stack) {
        return stack.getOrDefault(Exposure.DataComponents.FILM_FRAME_SIZE, super.getFrameSize(stack));
    }

    @Override
    public int getMaxFrameCount(ItemStack stack) {
        int frameCount = getStoredFramesCount(stack);
        return frameCount > 0 ? frameCount : super.getMaxFrameCount(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        DynamicFilmItem.syncExposureCompatibilityComponents(stack, getStoredFramesCount(stack), getFrameSize(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
                                TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        DynamicPhotographSummary summary = stack.getOrDefault(ExposureCamcorder.DataComponents.DYNAMIC_PHOTOGRAPH_SUMMARY,
                new DynamicPhotographSummary(0, 0, 0));
        DynamicPhotographSettings settings = stack.getOrDefault(ExposureCamcorder.DataComponents.DYNAMIC_PHOTOGRAPH_SETTINGS,
                DynamicPhotographSettings.DEFAULT);
        if (summary.frameCount() > 0) {
            tooltipComponents.add(Component.translatable("item.exposure_camcorder.dynamic_photograph.tooltip.capture_interval",
                            settings.captureIntervalTicks())
                    .withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.translatable("item.exposure_camcorder.dynamic_photograph.tooltip.duration",
                            DynamicPhotographTiming.formatSeconds(
                                    DynamicPhotographTiming.ticksToSeconds(summary.estimatedDurationTicks())))
                    .withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.translatable("item.exposure_camcorder.developed_dynamic_film.tooltip.use")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemInHand = player.getItemInHand(hand);
        if (getStoredFramesCount(itemInHand) <= 0) {
            return InteractionResultHolder.pass(itemInHand);
        }

        if (!level.isClientSide) {
            ItemStack photograph = itemInHand.transmuteCopy(ExposureCamcorder.Items.DYNAMIC_PHOTOGRAPH.get());
            photograph.remove(ExposureCamcorder.DataComponents.DYNAMIC_FILM_MAX_FRAMES);
            photograph.remove(ExposureCamcorder.DataComponents.DYNAMIC_FILM_USED_FRAMES);
            photograph.remove(Exposure.DataComponents.FILM_FRAME_COUNT);
            photograph.remove(Exposure.DataComponents.FILM_FRAME_SIZE);
            player.setItemInHand(hand, photograph);
        }

        return InteractionResultHolder.sidedSuccess(itemInHand, level.isClientSide);
    }
}
