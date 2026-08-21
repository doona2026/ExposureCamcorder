package io.github.exposure_camcorder.fabric.network;

import io.github.exposure_camcorder.network.packet.c2s.DynamicCameraModeToggleC2SP;
import io.github.exposure_camcorder.network.packet.c2s.DynamicCaptureFrameDataC2SP;
import io.github.exposure_camcorder.network.packet.c2s.DynamicCaptureStopC2SP;
import io.github.exposure_camcorder.network.packet.s2c.DynamicCaptureFrameRequestS2CP;
import io.github.exposure_camcorder.network.packet.s2c.DynamicCaptureStartS2CP;
import io.github.exposure_camcorder.network.packet.s2c.DynamicCaptureStateS2CP;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;

public final class FabricPackets {
    private FabricPackets() {
    }

    public static void registerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(DynamicCameraModeToggleC2SP.ID, FabricPackets::handleServerboundToggle);
        ServerPlayNetworking.registerGlobalReceiver(DynamicCaptureFrameDataC2SP.ID, FabricPackets::handleServerboundFrameData);
        ServerPlayNetworking.registerGlobalReceiver(DynamicCaptureStopC2SP.ID, FabricPackets::handleServerboundStop);
    }

    public static void registerClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(DynamicCaptureStartS2CP.ID, FabricPackets::handleClientboundStart);
        ClientPlayNetworking.registerGlobalReceiver(DynamicCaptureFrameRequestS2CP.ID, FabricPackets::handleClientboundFrameRequest);
        ClientPlayNetworking.registerGlobalReceiver(DynamicCaptureStateS2CP.ID, FabricPackets::handleClientboundState);
    }

    private static void handleServerboundToggle(MinecraftServer server, ServerPlayer player,
                                                 ServerGamePacketListenerImpl handler,
                                                 FriendlyByteBuf buf, net.fabricmc.fabric.api.networking.v1.PacketSender responseSender) {
        DynamicCameraModeToggleC2SP packet = DynamicCameraModeToggleC2SP.read(buf);
        server.execute(() -> packet.handle(PacketFlow.SERVERBOUND, player));
    }

    private static void handleServerboundFrameData(MinecraftServer server, ServerPlayer player,
                                                   ServerGamePacketListenerImpl handler,
                                                   FriendlyByteBuf buf, net.fabricmc.fabric.api.networking.v1.PacketSender responseSender) {
        DynamicCaptureFrameDataC2SP packet = DynamicCaptureFrameDataC2SP.read(buf);
        server.execute(() -> packet.handle(PacketFlow.SERVERBOUND, player));
    }

    private static void handleServerboundStop(MinecraftServer server, ServerPlayer player,
                                              ServerGamePacketListenerImpl handler,
                                              FriendlyByteBuf buf, net.fabricmc.fabric.api.networking.v1.PacketSender responseSender) {
        DynamicCaptureStopC2SP packet = DynamicCaptureStopC2SP.read(buf);
        server.execute(() -> packet.handle(PacketFlow.SERVERBOUND, player));
    }

    private static void handleClientboundStart(Minecraft client, ClientPacketListener handler,
                                                FriendlyByteBuf buf, net.fabricmc.fabric.api.networking.v1.PacketSender responseSender) {
        DynamicCaptureStartS2CP packet = DynamicCaptureStartS2CP.read(buf);
        client.execute(() -> packet.handle(PacketFlow.CLIENTBOUND, client.player));
    }

    private static void handleClientboundFrameRequest(Minecraft client, ClientPacketListener handler,
                                                      FriendlyByteBuf buf, net.fabricmc.fabric.api.networking.v1.PacketSender responseSender) {
        DynamicCaptureFrameRequestS2CP packet = DynamicCaptureFrameRequestS2CP.read(buf);
        client.execute(() -> packet.handle(PacketFlow.CLIENTBOUND, client.player));
    }

    private static void handleClientboundState(Minecraft client, ClientPacketListener handler,
                                                FriendlyByteBuf buf, net.fabricmc.fabric.api.networking.v1.PacketSender responseSender) {
        DynamicCaptureStateS2CP packet = DynamicCaptureStateS2CP.read(buf);
        client.execute(() -> packet.handle(PacketFlow.CLIENTBOUND, client.player));
    }
}
