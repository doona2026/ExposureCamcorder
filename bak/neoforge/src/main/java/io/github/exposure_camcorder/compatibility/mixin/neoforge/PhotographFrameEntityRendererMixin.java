package io.github.exposure_camcorder.compatibility.mixin.neoforge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.exposure_camcorder.client.playback.DynamicPhotographDisplayPlaybackManager;
import io.github.exposure_camcorder.compatibility.exposure.ExposurePhotographScreenHooks;
import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.client.image.modifier.ImageEffect;
import io.github.mortuusars.exposure.client.image.renderable.RenderableImage;
import io.github.mortuusars.exposure.client.render.PhotographFrameEntityRenderer;
import io.github.mortuusars.exposure.client.render.image.RenderCoordinates;
import io.github.mortuusars.exposure.client.render.photograph.PhotographStyle;
import io.github.mortuusars.exposure.world.camera.frame.Frame;
import io.github.mortuusars.exposure.world.entity.PhotographFrameEntity;
import io.github.mortuusars.exposure.world.item.PhotographItem;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PhotographFrameEntityRenderer.class, remap = false)
public abstract class PhotographFrameEntityRendererMixin {
    @Shadow public abstract int getPhotographBrightness(PhotographFrameEntity entity);

    @Inject(method = "renderPhotograph", at = @At("HEAD"), cancellable = true, remap = false)
    private void exposureCamcorder$renderDynamicPhotograph(PhotographFrameEntity entity, PoseStack poseStack,
                                                           MultiBufferSource bufferSource, int packedLight,
                                                           ItemStack item, int size,
                                                           CallbackInfoReturnable<Boolean> cir) {
        if (!ExposurePhotographScreenHooks.isDynamicPhotograph(item)) {
            return;
        }

        ItemStack displayStack = DynamicPhotographDisplayPlaybackManager.getInstance().resolveFrameDisplayStack(entity, item);

        poseStack.pushPose();

        boolean frameInvisible = entity.isFrameInvisible();

        float frameBorderOffset = frameInvisible ? 0f : 0.125f;
        float offsetFromCenter = frameInvisible ? 0.497f : 0.48f;
        offsetFromCenter -= Config.Client.PHOTOGRAPH_FRAME_IMAGE_OFFSET.get();
        float desiredSize = size + 1 - frameBorderOffset * 2;

        poseStack.mulPose(Axis.ZP.rotationDegrees((entity.getItemRotation() * 360.0F / 4.0F)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.translate(-0.5 * (size + 1) + frameBorderOffset, -0.5 * (size + 1) + frameBorderOffset, offsetFromCenter);
        poseStack.scale(desiredSize, desiredSize, 1);

        boolean isGlowing = entity.isGlowing();
        if (isGlowing) {
            packedLight = LightTexture.FULL_BRIGHT;
        }

        int brightness = isGlowing ? 255 : getPhotographBrightness(entity);
        boolean photographRendered = false;

        if (Config.Client.PIXEL_PERFECT_PHOTOGRAPH_FRAME.get()) {
            if (displayStack.getItem() instanceof PhotographItem photographItem) {
                PhotographStyle style = PhotographStyle.of(displayStack);
                Frame frame = photographItem.getFrame(displayStack);

                RenderableImage image = style.process(ExposureClient.renderedExposures().getOrCreate(frame));

                int pixels = 16 * (entity.getSize() + 1);
                if (!frameInvisible) {
                    pixels -= 4;
                }
                image = image.modifyWith(ImageEffect.Resize.to(pixels)::modify, "pixels-" + pixels);

                ExposureClient.imageRenderer().render(image, poseStack, bufferSource, RenderCoordinates.DEFAULT,
                        packedLight, brightness, brightness, brightness, 255);
                photographRendered = !image.isEmpty();
            }
        } else {
            photographRendered = ExposureClient.photographRenderer().render(displayStack, false, false,
                    poseStack, bufferSource, packedLight, brightness, brightness, brightness, 255);
        }

        poseStack.popPose();
        cir.setReturnValue(photographRendered);
    }
}
