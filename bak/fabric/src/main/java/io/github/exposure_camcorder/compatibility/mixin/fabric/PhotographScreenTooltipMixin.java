package io.github.exposure_camcorder.compatibility.mixin.fabric;

import io.github.exposure_camcorder.compatibility.exposure.ExposurePhotographScreenHooks;
import io.github.mortuusars.exposure.client.gui.screen.PhotographScreen;
import io.github.mortuusars.exposure.world.camera.frame.Frame;
import io.github.mortuusars.exposure.world.item.PhotographItem;
import io.github.mortuusars.exposure.world.item.util.ItemAndStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(PhotographScreen.class)
public abstract class PhotographScreenTooltipMixin {
    @Inject(
            method = "renderFrameInfoHint",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;II)V"),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void exposureCamcorder$appendDynamicPhotographExportTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY,
                                                                        ItemAndStack<PhotographItem> photograph,
                                                                        CallbackInfo ci, Frame frame, String exposureName,
                                                                        List<Component> lines) {
        ExposurePhotographScreenHooks.appendDynamicPhotographTooltipLines((PhotographScreen) (Object) this, lines);
    }
}
