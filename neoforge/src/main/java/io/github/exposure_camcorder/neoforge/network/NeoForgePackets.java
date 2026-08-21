package io.github.exposure_camcorder.neoforge.network;

import io.github.exposure_camcorder.network.packet.HandledPayload;
import io.github.exposure_camcorder.network.packet.c2s.DynamicCameraModeToggleC2SP;
import io.github.exposure_camcorder.network.packet.c2s.DynamicCaptureFrameDataC2SP;
import io.github.exposure_camcorder.network.packet.c2s.DynamicCaptureHeartbeatC2SP;
import io.github.exposure_camcorder.network.packet.c2s.DynamicCaptureStopC2SP;
import io.github.exposure_camcorder.network.packet.s2c.DynamicCaptureStartS2CP;
import io.github.exposure_camcorder.network.packet.s2c.DynamicCaptureStateS2CP;
import net.minecraft.network.protocol.PacketFlow;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NeoForgePackets {
    private NeoForgePackets() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").executesOn(HandlerThread.MAIN);

        registrar.playToClient(DynamicCaptureStartS2CP.TYPE, DynamicCaptureStartS2CP.STREAM_CODEC,
                NeoForgePackets::handleClientboundPacket);
        registrar.playToClient(DynamicCaptureStateS2CP.TYPE, DynamicCaptureStateS2CP.STREAM_CODEC,
                NeoForgePackets::handleClientboundPacket);

        registrar.playToServer(DynamicCameraModeToggleC2SP.TYPE, DynamicCameraModeToggleC2SP.STREAM_CODEC,
                NeoForgePackets::handleServerboundPacket);
        registrar.playToServer(DynamicCaptureFrameDataC2SP.TYPE, DynamicCaptureFrameDataC2SP.STREAM_CODEC,
                NeoForgePackets::handleServerboundPacket);
        registrar.playToServer(DynamicCaptureHeartbeatC2SP.TYPE, DynamicCaptureHeartbeatC2SP.STREAM_CODEC,
                NeoForgePackets::handleServerboundPacket);
        registrar.playToServer(DynamicCaptureStopC2SP.TYPE, DynamicCaptureStopC2SP.STREAM_CODEC,
                NeoForgePackets::handleServerboundPacket);
    }

    private static <T extends HandledPayload> void handleServerboundPacket(T payload, IPayloadContext context) {
        payload.handle(PacketFlow.SERVERBOUND, context.player());
    }

    private static <T extends HandledPayload> void handleClientboundPacket(T payload, IPayloadContext context) {
        payload.handle(PacketFlow.CLIENTBOUND, context.player());
    }
}
