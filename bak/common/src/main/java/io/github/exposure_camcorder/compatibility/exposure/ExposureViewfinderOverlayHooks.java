package io.github.exposure_camcorder.compatibility.exposure;

import io.github.exposure_camcorder.client.capture.DynamicFrameCaptureClient;
import io.github.exposure_camcorder.client.capture.DynamicRecordingClientState;
import io.github.exposure_camcorder.client.gui.component.DynamicRecordingStatusOverlay;
import io.github.exposure_camcorder.world.item.camera.DynamicCameraModeController;
import io.github.mortuusars.exposure.client.util.Minecrft;
import io.github.mortuusars.exposure.util.Rect2f;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public final class ExposureViewfinderOverlayHooks {
    private static final DynamicCameraModeController MODE_CONTROLLER = new DynamicCameraModeController();
    private static final DynamicRecordingStatusOverlay RECORDING_OVERLAY = new DynamicRecordingStatusOverlay();

    private ExposureViewfinderOverlayHooks() {
    }

    public static void renderRecordingHud(GuiGraphics guiGraphics, ItemStack cameraStack, Rect2f opening) {
        if (!MODE_CONTROLLER.getState(cameraStack).isDynamic()) {
            return;
        }

        DynamicRecordingClientState recordingState = DynamicFrameCaptureClient.getRecordingState();
        if (!RECORDING_OVERLAY.shouldRender(recordingState)) {
            return;
        }

        int panelWidth = RECORDING_OVERLAY.getPanelWidth(Minecrft.get().font, recordingState);
        int x = Math.max(8, (int) opening.x - panelWidth - RECORDING_OVERLAY.getViewfinderOffsetX());
        int y = (int) opening.y + RECORDING_OVERLAY.getViewfinderOffsetY();
        RECORDING_OVERLAY.render(guiGraphics, Minecrft.get().font, x, y, recordingState);
    }
}
