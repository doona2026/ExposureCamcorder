package io.github.exposure_camcorder.network.packet;

import io.github.exposure_camcorder.network.packet.c2s.DynamicCaptureFrameDataC2SP;
import io.github.exposure_camcorder.network.packet.c2s.DynamicCaptureStopC2SP;
import io.github.exposure_camcorder.network.packet.c2s.DynamicCameraModeToggleC2SP;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public class C2SPackets {
    public static List<CustomPacketPayload.TypeAndCodec<? extends FriendlyByteBuf, ? extends CustomPacketPayload>> getDefinitions() {
        return List.of(
                new CustomPacketPayload.TypeAndCodec<>(DynamicCameraModeToggleC2SP.TYPE, DynamicCameraModeToggleC2SP.STREAM_CODEC),
                new CustomPacketPayload.TypeAndCodec<>(DynamicCaptureFrameDataC2SP.TYPE, DynamicCaptureFrameDataC2SP.STREAM_CODEC),
                new CustomPacketPayload.TypeAndCodec<>(DynamicCaptureStopC2SP.TYPE, DynamicCaptureStopC2SP.STREAM_CODEC)
        );
    }
}
