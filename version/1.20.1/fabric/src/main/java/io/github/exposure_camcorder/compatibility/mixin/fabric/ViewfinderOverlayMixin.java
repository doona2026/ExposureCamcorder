package io.github.exposure_camcorder.compatibility.mixin.fabric;

import io.github.exposure_camcorder.client.capture.ViewfinderDirectScreenshotCaptureTask;
import io.github.exposure_camcorder.compatibility.exposure.ExposureViewfinderOverlayHooks;
import io.github.mortuusars.exposure.client.camera.viewfinder.ViewfinderOverlay;
import io.github.mortuusars.exposure.util.Rect2f;
import io.github.mortuusars.exposure.world.camera.Camera;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ViewfinderOverlay.class)
public abstract class ViewfinderOverlayMixin {
    @Shadow @Final protected Camera camera;
    @Shadow @Final protected Rect2f opening;

    @Inject(method = "render", at = @At("HEAD"))
    private void exposureCamcorder$captureDynamicFrame(GuiGraphics guiGraphics, float tickDelta, CallbackInfo ci) {
        ViewfinderDirectScreenshotCaptureTask.capturePending();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void exposureCamcorder$renderDynamicRecordingHud(GuiGraphics guiGraphics, float tickDelta, CallbackInfo ci) {
        ExposureViewfinderOverlayHooks.renderRecordingHud(guiGraphics, camera.getItemStack(), opening);
    }
}
