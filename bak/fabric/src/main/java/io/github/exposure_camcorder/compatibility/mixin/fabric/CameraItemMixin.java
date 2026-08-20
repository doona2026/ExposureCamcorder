package io.github.exposure_camcorder.compatibility.mixin.fabric;

import io.github.exposure_camcorder.compatibility.exposure.ExposureCameraHooks;
import io.github.mortuusars.exposure.world.entity.CameraHolder;
import io.github.mortuusars.exposure.world.item.camera.CameraItem;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CameraItem.class)
public abstract class CameraItemMixin {
    @Inject(method = "inventoryTick", at = @At("HEAD"))
    private void exposureCamcorder$normalizePersistedRuntimeState(ItemStack stack, Level level, Entity entity,
                                                                  int slotId, boolean isSelected, CallbackInfo ci) {
        if (entity instanceof Player player) {
            ExposureCameraHooks.normalizeCameraRuntimeState(level, player, stack);
        }
    }

    @Inject(method = "release", at = @At("HEAD"), cancellable = true)
    private void exposureCamcorder$startDynamicRecording(CameraHolder holder, ItemStack stack,
                                                         CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (!(holder.asHolderEntity() instanceof Player player)) {
            return;
        }

        if (ExposureCameraHooks.handleDynamicCameraRelease(player.level(), player, stack)) {
            cir.setReturnValue(InteractionResultHolder.consume(stack));
        }
    }
}
