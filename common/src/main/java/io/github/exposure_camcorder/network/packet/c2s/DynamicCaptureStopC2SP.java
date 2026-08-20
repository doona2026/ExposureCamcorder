package io.github.exposure_camcorder.network.packet.c2s;

import io.github.exposure_camcorder.ExposureCamcorder;
import io.github.exposure_camcorder.compatibility.exposure.ExposureAccess;
import io.github.exposure_camcorder.network.packet.HandledPayload;
import io.github.exposure_camcorder.world.session.DynamicCaptureSessionEndReason;
import io.github.mortuusars.exposure.network.packet.Packet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public record DynamicCaptureStopC2SP(String sessionId, DynamicCaptureSessionEndReason reason) implements Packet, HandledPayload, CustomPacketPayload {
    public static final ResourceLocation ID = ExposureCamcorder.resource("dynamic_capture_stop");
    public static final Type<DynamicCaptureStopC2SP> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, DynamicCaptureStopC2SP> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, DynamicCaptureStopC2SP::sessionId,
            new StreamCodec<>() {
                @Override
                public DynamicCaptureSessionEndReason decode(FriendlyByteBuf buffer) {
                    return DynamicCaptureSessionEndReason.byName(buffer.readUtf());
                }

                @Override
                public void encode(FriendlyByteBuf buffer, DynamicCaptureSessionEndReason value) {
                    buffer.writeUtf(value.name());
                }
            }, DynamicCaptureStopC2SP::reason,
            DynamicCaptureStopC2SP::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public boolean handle(PacketFlow flow, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        return ExposureAccess.requestStop(serverPlayer, sessionId, reason);
    }
}
