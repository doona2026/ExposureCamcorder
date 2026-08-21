package io.github.exposure_camcorder.network.packet.c2s;

import io.github.exposure_camcorder.ExposureCamcorder;
import io.github.exposure_camcorder.network.packet.HandledPayload;
import io.github.exposure_camcorder.world.session.DynamicCaptureSessionEndReason;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public record DynamicCaptureStopC2SP(String sessionId, DynamicCaptureSessionEndReason reason) implements HandledPayload {
    public static final ResourceLocation ID = ExposureCamcorder.resource("dynamic_capture_stop");

    public static DynamicCaptureStopC2SP read(FriendlyByteBuf buffer) {
        return new DynamicCaptureStopC2SP(buffer.readUtf(128), DynamicCaptureSessionEndReason.byName(buffer.readUtf(64)));
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(sessionId, 128);
        buffer.writeUtf(reason.name(), 64);
    }

    @Override
    public boolean handle(PacketFlow flow, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        ExposureCamcorder.captureSessionManager().requestStop(serverPlayer.getUUID(), reason);
        return true;
    }
}
