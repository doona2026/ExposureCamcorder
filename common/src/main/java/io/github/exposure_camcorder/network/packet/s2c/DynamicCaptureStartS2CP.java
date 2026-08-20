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

public record DynamicCaptureStartS2CP(String sessionId, int captureIntervalTicks, int maxFrames,
                                      int maxRecordingDurationTicks, CaptureParameters captureParameters)
        implements Packet, HandledPayload, CustomPacketPayload {
    public static final ResourceLocation ID = ExposureCamcorder.resource("dynamic_capture_start");
    public static final Type<DynamicCaptureStartS2CP> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, DynamicCaptureStartS2CP> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, DynamicCaptureStartS2CP::sessionId,
            ByteBufCodecs.VAR_INT, DynamicCaptureStartS2CP::captureIntervalTicks,
            ByteBufCodecs.VAR_INT, DynamicCaptureStartS2CP::maxFrames,
            ByteBufCodecs.VAR_INT, DynamicCaptureStartS2CP::maxRecordingDurationTicks,
            CaptureParameters.STREAM_CODEC, DynamicCaptureStartS2CP::captureParameters,
            DynamicCaptureStartS2CP::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public boolean handle(PacketFlow flow, Player player) {
        DynamicFrameCaptureClient.startSession(this);
        return true;
    }
}
