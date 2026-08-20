package io.github.exposure_camcorder.network.packet;

import io.github.exposure_camcorder.network.packet.s2c.DynamicCaptureStartS2CP;
import io.github.exposure_camcorder.network.packet.s2c.DynamicCaptureStateS2CP;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public class S2CPackets {
    public static List<CustomPacketPayload.TypeAndCodec<? extends FriendlyByteBuf, ? extends CustomPacketPayload>> getDefinitions() {
        return List.of(
                new CustomPacketPayload.TypeAndCodec<>(DynamicCaptureStartS2CP.TYPE, DynamicCaptureStartS2CP.STREAM_CODEC),
                new CustomPacketPayload.TypeAndCodec<>(DynamicCaptureStateS2CP.TYPE, DynamicCaptureStateS2CP.STREAM_CODEC)
        );
    }
}
