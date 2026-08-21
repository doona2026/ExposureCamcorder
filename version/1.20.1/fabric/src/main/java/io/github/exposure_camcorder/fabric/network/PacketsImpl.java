package io.github.exposure_camcorder.fabric.network;

import io.github.exposure_camcorder.network.packet.HandledPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public final class PacketsImpl {
    private PacketsImpl() {
    }

    public static void sendToServer(HandledPayload packet) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        packet.write(buffer);
        ClientPlayNetworking.send(packet.getId(), buffer);
    }

    public static void sendToClient(HandledPayload packet, ServerPlayer player) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        packet.write(buffer);
        ServerPlayNetworking.send(player, packet.getId(), buffer);
    }
}
