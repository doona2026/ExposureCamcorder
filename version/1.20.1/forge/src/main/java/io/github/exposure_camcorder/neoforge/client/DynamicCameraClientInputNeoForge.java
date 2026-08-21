package io.github.exposure_camcorder.neoforge.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.exposure_camcorder.client.capture.DynamicFrameCaptureClient;
import io.github.exposure_camcorder.client.playback.DynamicPhotographDisplayPlaybackManager;
import io.github.exposure_camcorder.client.input.DynamicCameraModeClient;
import io.github.exposure_camcorder.world.item.DynamicMode;
import io.github.exposure_camcorder.world.item.camera.DynamicCameraModeController;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;

public final class DynamicCameraClientInputNeoForge {
    private static final ResourceLocation EXPOSURE_CAMERA_ID =
            new ResourceLocation("exposure", "camera");
    private static final DynamicCameraModeController MODE_CONTROLLER = new DynamicCameraModeController();
    private static final KeyMapping TOGGLE_DYNAMIC_MODE_KEY = new KeyMapping(
            "key.exposure_camcorder.toggle_dynamic_mode",
            InputConstants.KEY_R,
            "category.exposure");
    private static final KeyMapping TOGGLE_DYNAMIC_DISPLAY_PLAYBACK_KEY = new KeyMapping(
            "key.exposure_camcorder.toggle_dynamic_display_playback",
            InputConstants.KEY_P,
            "category.exposure");

    private DynamicCameraClientInputNeoForge() {
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_DYNAMIC_MODE_KEY);
        event.register(TOGGLE_DYNAMIC_DISPLAY_PLAYBACK_KEY);
    }

    public static void onClientTick(ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.phase == TickEvent.Phase.START) {
            DynamicFrameCaptureClient.trackKeyState(minecraft);
            return;
        }

        DynamicFrameCaptureClient.tick();
        DynamicPhotographDisplayPlaybackManager.getInstance().tick(minecraft);

        while (TOGGLE_DYNAMIC_MODE_KEY.consumeClick()) {
            ItemStack cameraStack = resolveToggleCamera(minecraft);
            if (!cameraStack.isEmpty()) {
                minecraft.gui.setOverlayMessage(
                        DynamicFrameCaptureClient.hasActiveSession()
                                ? Component.translatable("message.exposure_camcorder.camera.recording.stopped")
                                : nextModeMessage(cameraStack), false);
                DynamicCameraModeClient.requestToggle();
            }
        }

        while (TOGGLE_DYNAMIC_DISPLAY_PLAYBACK_KEY.consumeClick()) {
            if (minecraft.screen == null) {
                DynamicPhotographDisplayPlaybackManager.getInstance().togglePlayback(minecraft);
            }
        }
    }

    public static boolean matchesDynamicDisplayPlaybackShortcut(int keyCode, int scanCode, int modifiers) {
        return TOGGLE_DYNAMIC_DISPLAY_PLAYBACK_KEY.matches(keyCode, scanCode);
    }

    private static boolean canToggleMode(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.screen != null) {
            return false;
        }

        return isExposureCamera(minecraft.player.getMainHandItem())
                || isExposureCamera(minecraft.player.getOffhandItem());
    }

    private static ItemStack resolveToggleCamera(Minecraft minecraft) {
        if (!canToggleMode(minecraft)) {
            return ItemStack.EMPTY;
        }

        ItemStack mainHandItem = minecraft.player.getMainHandItem();
        if (isExposureCamera(mainHandItem)) {
            return mainHandItem;
        }

        ItemStack offhandItem = minecraft.player.getOffhandItem();
        return isExposureCamera(offhandItem) ? offhandItem : ItemStack.EMPTY;
    }

    private static boolean isExposureCamera(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(EXPOSURE_CAMERA_ID);
    }

    private static Component nextModeMessage(ItemStack cameraStack) {
        DynamicMode nextMode = MODE_CONTROLLER.getState(cameraStack).mode().next();
        return Component.translatable(nextMode == DynamicMode.DYNAMIC
                ? "message.exposure_camcorder.camera.mode.dynamic"
                : "message.exposure_camcorder.camera.mode.regular");
    }
}
