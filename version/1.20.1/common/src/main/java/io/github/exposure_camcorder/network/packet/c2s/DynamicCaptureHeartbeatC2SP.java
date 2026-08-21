package io.github.exposure_camcorder.network.packet.c2s;

import io.github.exposure_camcorder.ExposureCamcorder;
import io.github.exposure_camcorder.compatibility.exposure.ExposureAccess;
import io.github.exposure_camcorder.network.packet.HandledPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public record DynamicCaptureHeartbeatC2SP(String sessionId) implements HandledPayload {
    public static final ResourceLocation ID = ExposureCamcorder.resource("dynamic_capture_heartbeat");

    public static DynamicCaptureHeartbeatC2SP read(FriendlyByteBuf buffer) {
        return new DynamicCaptureHeartbeatC2SP(buffer.readUtf(128));
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(sessionId, 128);
    }

    @Override
    public boolean handle(PacketFlow flow, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        return ExposureAccess.receiveHeartbeat(serverPlayer, sessionId);
    }
}
