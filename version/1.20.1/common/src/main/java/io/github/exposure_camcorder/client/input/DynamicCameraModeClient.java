package io.github.exposure_camcorder.client.input;

import io.github.exposure_camcorder.network.packet.c2s.DynamicCameraModeToggleC2SP;
import io.github.exposure_camcorder.network.Packets;

public final class DynamicCameraModeClient {
    private DynamicCameraModeClient() {
    }

    public static void requestToggle() {
        Packets.sendToServer(DynamicCameraModeToggleC2SP.INSTANCE);
    }
}
