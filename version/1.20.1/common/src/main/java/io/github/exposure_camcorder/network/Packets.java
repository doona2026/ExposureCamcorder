package io.github.exposure_camcorder.network;

import dev.architectury.injectables.annotations.ExpectPlatform;
import io.github.exposure_camcorder.network.packet.HandledPayload;
import net.minecraft.server.level.ServerPlayer;

public final class Packets {
    private Packets() {
    }

    @ExpectPlatform
    public static void sendToServer(HandledPayload packet) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void sendToClient(HandledPayload packet, ServerPlayer player) {
        throw new AssertionError();
    }
}
