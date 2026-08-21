package io.github.exposure_camcorder.neoforge.network;

import io.github.exposure_camcorder.ExposureCamcorder;
import io.github.exposure_camcorder.network.packet.HandledPayload;
import io.github.exposure_camcorder.network.packet.c2s.DynamicCameraModeToggleC2SP;
import io.github.exposure_camcorder.network.packet.c2s.DynamicCaptureFrameDataC2SP;
import io.github.exposure_camcorder.network.packet.c2s.DynamicCaptureHeartbeatC2SP;
import io.github.exposure_camcorder.network.packet.c2s.DynamicCaptureStopC2SP;
import io.github.exposure_camcorder.network.packet.s2c.DynamicCaptureStartS2CP;
import io.github.exposure_camcorder.network.packet.s2c.DynamicCaptureStateS2CP;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Function;

public final class NeoForgePackets {
    private static final String PROTOCOL_VERSION = "1";
    static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ExposureCamcorder.resource("network"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);
    private static boolean registered;

    private NeoForgePackets() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        int index = 0;
        registerClientbound(index++, DynamicCaptureStartS2CP.class, DynamicCaptureStartS2CP::read);
        registerClientbound(index++, DynamicCaptureStateS2CP.class, DynamicCaptureStateS2CP::read);
        registerServerbound(index++, DynamicCameraModeToggleC2SP.class, DynamicCameraModeToggleC2SP::read);
        registerServerbound(index++, DynamicCaptureFrameDataC2SP.class, DynamicCaptureFrameDataC2SP::read);
        registerServerbound(index++, DynamicCaptureHeartbeatC2SP.class, DynamicCaptureHeartbeatC2SP::read);
        registerServerbound(index++, DynamicCaptureStopC2SP.class, DynamicCaptureStopC2SP::read);
    }

    public static void sendToServer(HandledPayload packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendToClient(HandledPayload packet, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    private static <T extends HandledPayload> void registerServerbound(int id, Class<T> type, Function<FriendlyByteBuf, T> decoder) {
        CHANNEL.messageBuilder(type, id, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buffer) -> packet.write(buffer))
                .decoder(decoder)
                .consumerMainThread((packet, ctx) -> {
                    ServerPlayer sender = ctx.get().getSender();
                    if (sender != null) {
                        packet.handle(PacketFlow.SERVERBOUND, sender);
                    }
                })
                .add();
    }

    private static <T extends HandledPayload> void registerClientbound(int id, Class<T> type, Function<FriendlyByteBuf, T> decoder) {
        CHANNEL.messageBuilder(type, id, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((packet, buffer) -> packet.write(buffer))
                .decoder(decoder)
                .consumerMainThread((packet, ctx) -> {
                    Minecraft client = Minecraft.getInstance();
                    if (client.player != null) {
                        packet.handle(PacketFlow.CLIENTBOUND, client.player);
                    }
                })
                .add();
    }
}
