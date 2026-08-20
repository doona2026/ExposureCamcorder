package io.github.exposure_camcorder.fabric;

import io.github.exposure_camcorder.ExposureCamcorderClient;
import io.github.exposure_camcorder.client.capture.DynamicFrameCaptureClient;
import io.github.exposure_camcorder.client.playback.DynamicPhotographDisplayPlaybackManager;
import io.github.exposure_camcorder.fabric.client.DynamicCameraClientInput;
import io.github.exposure_camcorder.fabric.network.FabricPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class ExposureCamcorderFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FabricPackets.registerClientReceivers();
        ExposureCamcorderClient.init();
        DynamicCameraClientInput.init();
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            DynamicFrameCaptureClient.reset();
            DynamicPhotographDisplayPlaybackManager.getInstance().reset();
        });
    }
}
