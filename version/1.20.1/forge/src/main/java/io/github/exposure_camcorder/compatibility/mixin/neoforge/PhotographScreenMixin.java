package io.github.exposure_camcorder.compatibility.mixin.neoforge;

import io.github.exposure_camcorder.compatibility.exposure.ExposurePhotographScreenHooks;
import io.github.mortuusars.exposure.client.gui.screen.PhotographScreen;
import io.github.mortuusars.exposure.client.render.photograph.PhotographRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import io.github.mortuusars.exposure.world.item.PhotographItem;
import io.github.mortuusars.exposure.world.item.util.ItemAndStack;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PhotographScreen.class)
public abstract class PhotographScreenMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private void exposureCamcorder$installDynamicPhotographControls(CallbackInfo ci) {
        ExposurePhotographScreenHooks.onPhotographScreenInit((PhotographScreen) (Object) this);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void exposureCamcorder$tickDynamicPhotograph(CallbackInfo ci) {
        ExposurePhotographScreenHooks.onPhotographScreenTick((PhotographScreen) (Object) this);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void exposureCamcorder$handleDynamicPhotographKeys(int keyCode, int scanCode, int modifiers,
                                                               CallbackInfoReturnable<Boolean> cir) {
        if (ExposurePhotographScreenHooks.onPhotographScreenKeyPressed((PhotographScreen) (Object) this,
                keyCode, scanCode, modifiers)) {
            cir.setReturnValue(true);
        }
    }

    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lio/github/mortuusars/exposure/client/render/photograph/PhotographRenderer;renderStackedPhotographs(Ljava/util/List;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IIIII)Z"))
    private boolean exposureCamcorder$renderDynamicPhotographScreen(PhotographRenderer renderer,
                                                                    List<ItemAndStack<PhotographItem>> photographs,
                                                                    PoseStack poseStack,
                                                                    MultiBufferSource bufferSource,
                                                                    int packedLight, int r, int g, int b, int a) {
        if (ExposurePhotographScreenHooks.renderDynamicPhotographScreen((PhotographScreen) (Object) this,
                poseStack, bufferSource, packedLight, r, g, b, a)) {
            return true;
        }
        return renderer.renderStackedPhotographs(photographs, poseStack, bufferSource, packedLight, r, g, b, a);
    }
}
