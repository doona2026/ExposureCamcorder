package io.github.exposure_camcorder.network.packet.s2c;

import io.github.exposure_camcorder.ExposureCamcorder;
import io.github.exposure_camcorder.client.capture.DynamicFrameCaptureClient;
import io.github.exposure_camcorder.network.packet.HandledPayload;
import io.github.mortuusars.exposure.network.packet.Packet;
import io.github.mortuusars.exposure.world.camera.capture.CaptureParameters;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public record DynamicCaptureFrameRequestS2CP(String sessionId, int frameIndex, String exposureId,
                                             CaptureParameters captureParameters)
        implements Packet, HandledPayload, CustomPacketPayload {
    public static final ResourceLocation ID = ExposureCamcorder.resource("dynamic_capture_frame_request");
    public static final Type<DynamicCaptureFrameRequestS2CP> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, DynamicCaptureFrameRequestS2CP> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, DynamicCaptureFrameRequestS2CP::sessionId,
                    ByteBufCodecs.VAR_INT, DynamicCaptureFrameRequestS2CP::frameIndex,
                    ByteBufCodecs.STRING_UTF8, DynamicCaptureFrameRequestS2CP::exposureId,
                    CaptureParameters.STREAM_CODEC, DynamicCaptureFrameRequestS2CP::captureParameters,
                    DynamicCaptureFrameRequestS2CP::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public boolean handle(PacketFlow flow, Player player) {
        DynamicFrameCaptureClient.captureFrame(this);
        return true;
    }
}
