package io.github.exposure_camcorder.fabric.network;

import io.github.exposure_camcorder.network.packet.HandledPayload;
import io.github.exposure_camcorder.network.packet.C2SPackets;
import io.github.exposure_camcorder.network.packet.S2CPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class FabricPackets {
    private FabricPackets() {
    }

    @SuppressWarnings("unchecked")
    public static void registerPayloadTypes() {
        for (var definition : S2CPackets.getDefinitions()) {
            PayloadTypeRegistry.playS2C().register(
                    (CustomPacketPayload.Type<CustomPacketPayload>) definition.type(),
                    (StreamCodec<FriendlyByteBuf, CustomPacketPayload>) definition.codec().cast());
        }

        for (var definition : C2SPackets.getDefinitions()) {
            PayloadTypeRegistry.playC2S().register(
                    (CustomPacketPayload.Type<CustomPacketPayload>) definition.type(),
                    (StreamCodec<FriendlyByteBuf, CustomPacketPayload>) definition.codec().cast());
        }
    }

    @SuppressWarnings("unchecked")
    public static void registerServerReceivers() {
        for (var definition : C2SPackets.getDefinitions()) {
            ServerPlayNetworking.registerGlobalReceiver(
                    (CustomPacketPayload.Type<HandledPayload>) definition.type(),
                    FabricPackets::handleServerboundPacket);
        }
    }

    @SuppressWarnings("unchecked")
    public static void registerClientReceivers() {
        for (var definition : S2CPackets.getDefinitions()) {
            ClientPlayNetworking.registerGlobalReceiver(
                    (CustomPacketPayload.Type<HandledPayload>) definition.type(),
                    FabricPackets::handleClientboundPacket);
        }
    }

    private static <T extends HandledPayload> void handleServerboundPacket(T payload, ServerPlayNetworking.Context context) {
        payload.handle(PacketFlow.SERVERBOUND, context.player());
    }

    private static <T extends HandledPayload> void handleClientboundPacket(T payload, ClientPlayNetworking.Context context) {
        payload.handle(PacketFlow.CLIENTBOUND, context.player());
    }
}
