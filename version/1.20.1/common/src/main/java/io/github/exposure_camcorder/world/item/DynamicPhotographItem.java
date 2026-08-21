package io.github.exposure_camcorder.world.item;

import io.github.exposure_camcorder.compatibility.exposure.ExposurePhotographScreenHooks;
import io.github.exposure_camcorder.util.DynamicItemStackData;
import io.github.exposure_camcorder.util.DynamicPhotographTiming;
import io.github.exposure_camcorder.world.component.DynamicPhotographFrames;
import io.github.exposure_camcorder.world.component.DynamicPhotographSettings;
import io.github.exposure_camcorder.world.component.DynamicPhotographSummary;
import io.github.exposure_camcorder.world.component.DynamicSessionId;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.world.camera.frame.Frame;
import io.github.mortuusars.exposure.world.inventory.tooltip.PhotographTooltip;
import io.github.mortuusars.exposure.world.item.PhotographItem;
import io.github.mortuusars.exposure.world.item.util.ItemAndStack;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class DynamicPhotographItem extends PhotographItem {
    public DynamicPhotographItem(Properties properties) {
        super(properties);
    }

    public DynamicPhotographFrames getFrames(ItemStack stack) {
        return DynamicItemStackData.getDynamicPhotographFrames(stack, DynamicPhotographFrames.EMPTY);
    }

    public DynamicPhotographSettings getSettings(ItemStack stack) {
        return DynamicItemStackData.getDynamicPhotographSettings(stack, DynamicPhotographSettings.DEFAULT);
    }

    public Optional<Frame> getPrimaryFrame(ItemStack stack) {
        DynamicPhotographSummary summary = DynamicItemStackData.getDynamicPhotographSummary(stack,
                new DynamicPhotographSummary(0, 0, 0));
        return getFrames(stack).getCoverFrame(summary.coverFrameIndex());
    }

    @Override
    public Frame getFrame(ItemStack stack) {
        return getPrimaryFrame(stack).orElse(Frame.EMPTY);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return getPrimaryFrame(stack)
                .map(this::createTooltipImage)
                .map(component -> (TooltipComponent) component);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        DynamicPhotographSummary summary = DynamicItemStackData.getDynamicPhotographSummary(stack,
                new DynamicPhotographSummary(0, 0, 0));
        DynamicPhotographSettings settings = getSettings(stack);

        tooltipComponents.add(Component.translatable("item.exposure_camcorder.dynamic_photograph.tooltip.frame_count",
                        summary.frameCount())
                .withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("item.exposure_camcorder.dynamic_photograph.tooltip.capture_interval",
                        settings.captureIntervalTicks())
                .withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("item.exposure_camcorder.dynamic_photograph.tooltip.duration",
                        DynamicPhotographTiming.formatSeconds(DynamicPhotographTiming.ticksToSeconds(summary.estimatedDurationTicks())))
                .withStyle(ChatFormatting.GRAY));

        if (tooltipFlag.isAdvanced()) {
            DynamicSessionId sessionId = DynamicItemStackData.getDynamicPhotographSessionId(stack, DynamicSessionId.EMPTY);
            if (!sessionId.value().isEmpty()) {
                tooltipComponents.add(Component.translatable("item.exposure_camcorder.dynamic_photograph.tooltip.session",
                                sessionId.value())
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    private PhotographTooltip createTooltipImage(Frame frame) {
        ItemStack photographStack = new ItemStack(Exposure.Items.PHOTOGRAPH.get());
        io.github.mortuusars.exposure.Exposure.DataComponents.setPhotographFrame(photographStack, frame);
        io.github.mortuusars.exposure.Exposure.DataComponents.setPhotographType(photographStack, frame.type());
        return new PhotographTooltip(List.of(new ItemAndStack<>(photographStack)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemInHand = player.getItemInHand(hand);
        if (getPrimaryFrame(itemInHand).isEmpty()) {
            return InteractionResultHolder.pass(itemInHand);
        }

        if (level.isClientSide) {
            int slot = hand == InteractionHand.OFF_HAND ? Inventory.SLOT_OFFHAND : player.getInventory().selected;
            if (ExposurePhotographScreenHooks.openDynamicPhotographFromInventorySlot(slot)) {
                player.playSound(Exposure.SoundEvents.PHOTOGRAPH_RUSTLE.get(), 0.6f, 1.1f);
            }
        }

        return InteractionResultHolder.success(itemInHand);
    }
}
