package io.github.exposure_camcorder.network.forge;

import io.github.exposure_camcorder.network.packet.HandledPayload;
import net.minecraft.server.level.ServerPlayer;

public final class PacketsImpl {
    private PacketsImpl() {
    }

    public static void sendToServer(HandledPayload packet) {
        io.github.exposure_camcorder.neoforge.network.PacketsImpl.sendToServer(packet);
    }

    public static void sendToClient(HandledPayload packet, ServerPlayer player) {
        io.github.exposure_camcorder.neoforge.network.PacketsImpl.sendToClient(packet, player);
    }
}
