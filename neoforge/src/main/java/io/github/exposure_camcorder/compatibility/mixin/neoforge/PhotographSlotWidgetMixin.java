package io.github.exposure_camcorder.compatibility.mixin.neoforge;

import io.github.exposure_camcorder.client.playback.DynamicPhotographDisplayPlaybackManager;
import io.github.exposure_camcorder.compatibility.exposure.ExposurePhotographScreenHooks;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.client.render.photograph.PhotographStyle;
import io.github.mortuusars.exposure.client.gui.screen.album.PhotographSlotWidget;
import io.github.mortuusars.exposure.client.util.Minecrft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PhotographSlotWidget.class, remap = false)
public abstract class PhotographSlotWidgetMixin {
    @Shadow @Final private Screen parent;
    @Shadow protected boolean hasPhotograph;
    @Shadow public abstract ItemStack getPhotograph();

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true, remap = true)
    private void exposureCamcorder$trackHover(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        PhotographSlotWidget widget = (PhotographSlotWidget) (Object) this;
        ItemStack photograph = getPhotograph();
        DynamicPhotographDisplayPlaybackManager.getInstance().trackSlotHover(parent, widget, photograph,
                widget.isMouseOver(mouseX, mouseY));
        ItemStack displayStack = DynamicPhotographDisplayPlaybackManager.getInstance().resolveSlotDisplayStack(widget, photograph);

        if (displayStack.getItem() instanceof io.github.mortuusars.exposure.world.item.PhotographItem) {
            hasPhotograph = true;

            PhotographStyle photographStyle = PhotographStyle.of(displayStack);
            guiGraphics.blit(photographStyle.albumPaperTexture(),
                    widget.getX(), widget.getY(), 0, 0, 0, widget.getWidth(), widget.getHeight(),
                    widget.getWidth(), widget.getHeight());

            guiGraphics.pose().pushPose();
            float scale = 96;
            guiGraphics.pose().translate(widget.getX() + 6, widget.getY() + 6, 1);
            guiGraphics.pose().scale(scale, scale, scale);
            MultiBufferSource.BufferSource bufferSource = Minecrft.get().renderBuffers().bufferSource();
            ExposureClient.photographRenderer().render(displayStack, false, false,
                    guiGraphics.pose(), bufferSource, LightTexture.FULL_BRIGHT);
            bufferSource.endBatch();
            guiGraphics.pose().popPose();

            if (photographStyle.hasAlbumOverlayTexture()) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, 0, 2);
                guiGraphics.blit(photographStyle.albumOverlayTexture(),
                        widget.getX(), widget.getY(), 0, 0, 0, widget.getWidth(), widget.getHeight(),
                        widget.getWidth(), widget.getHeight());
                guiGraphics.pose().popPose();
            }
        } else {
            hasPhotograph = false;
        }

        ResourceLocation resourceLocation = (hasPhotograph
                ? PhotographSlotWidget.SPRITES
                : PhotographSlotWidget.EMPTY_SPRITES).get(widget.isActive(), widget.isHoveredOrFocused());
        if (!widget.isEditable() && !hasPhotograph) {
            resourceLocation = (hasPhotograph
                    ? PhotographSlotWidget.SPRITES
                    : PhotographSlotWidget.EMPTY_SPRITES).get(widget.isActive(), false);
        }
        guiGraphics.blitSprite(resourceLocation, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight());
        ci.cancel();
    }

    @Inject(method = "inspectPhotograph", at = @At("HEAD"), cancellable = true, remap = false)
    private void exposureCamcorder$openDynamicPhotograph(CallbackInfoReturnable<Boolean> cir) {
        ItemStack photograph = ((PhotographSlotWidget) (Object) this).getPhotograph();
        if (ExposurePhotographScreenHooks.openDynamicPhotograph(photograph)) {
            ExposurePhotographScreenHooks.playOpenSound();
            cir.setReturnValue(true);
        }
    }
}
