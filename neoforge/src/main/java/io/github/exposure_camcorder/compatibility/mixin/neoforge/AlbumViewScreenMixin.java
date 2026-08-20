package io.github.exposure_camcorder.compatibility.mixin.neoforge;

import io.github.exposure_camcorder.client.playback.DynamicPhotographDisplayPlaybackManager;
import io.github.exposure_camcorder.neoforge.client.DynamicCameraClientInputNeoForge;
import io.github.mortuusars.exposure.client.gui.screen.album.AlbumViewScreen;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AlbumViewScreen.class, remap = false)
public abstract class AlbumViewScreenMixin {
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, remap = true)
    private void exposureCamcorder$toggleDynamicPlayback(int keyCode, int scanCode, int modifiers,
                                                         CallbackInfoReturnable<Boolean> cir) {
        Minecraft minecraft = Minecraft.getInstance();
        if (DynamicCameraClientInputNeoForge.matchesDynamicDisplayPlaybackShortcut(keyCode, scanCode, modifiers)
                && DynamicPhotographDisplayPlaybackManager.getInstance().togglePlayback(minecraft)) {
            cir.setReturnValue(true);
        }
    }
}
