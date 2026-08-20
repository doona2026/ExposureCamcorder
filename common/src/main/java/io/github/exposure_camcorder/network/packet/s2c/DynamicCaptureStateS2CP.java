package io.github.exposure_camcorder.network.packet.s2c;

import io.github.exposure_camcorder.ExposureCamcorder;
import io.github.exposure_camcorder.client.capture.DynamicFrameCaptureClient;
import io.github.exposure_camcorder.network.packet.HandledPayload;
import io.github.mortuusars.exposure.network.packet.Packet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public record DynamicCaptureStateS2CP(String sessionId, int recordedFrames, boolean stopping)
        implements Packet, HandledPayload, CustomPacketPayload {
    public static final ResourceLocation ID = ExposureCamcorder.resource("dynamic_capture_state");
    public static final Type<DynamicCaptureStateS2CP> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, DynamicCaptureStateS2CP> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, DynamicCaptureStateS2CP::sessionId,
            ByteBufCodecs.VAR_INT, DynamicCaptureStateS2CP::recordedFrames,
            ByteBufCodecs.BOOL, DynamicCaptureStateS2CP::stopping,
            DynamicCaptureStateS2CP::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public boolean handle(PacketFlow flow, Player player) {
        DynamicFrameCaptureClient.updateState(this);
        return true;
    }
}
