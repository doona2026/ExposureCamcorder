package io.github.exposure_camcorder.neoforge.network;

import io.github.exposure_camcorder.network.packet.HandledPayload;
import net.minecraft.server.level.ServerPlayer;

public final class PacketsImpl {
    private PacketsImpl() {
    }

    public static void sendToServer(HandledPayload packet) {
        NeoForgePackets.sendToServer(packet);
    }

    public static void sendToClient(HandledPayload packet, ServerPlayer player) {
        NeoForgePackets.sendToClient(packet, player);
    }
}
