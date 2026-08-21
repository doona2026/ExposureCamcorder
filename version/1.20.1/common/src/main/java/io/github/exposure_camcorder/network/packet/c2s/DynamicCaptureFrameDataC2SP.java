package io.github.exposure_camcorder.network.packet.c2s;

import io.github.exposure_camcorder.ExposureCamcorder;
import io.github.exposure_camcorder.compatibility.exposure.ExposureAccess;
import io.github.exposure_camcorder.network.packet.HandledPayload;
import io.github.mortuusars.exposure.world.level.storage.ExposureData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public record DynamicCaptureFrameDataC2SP(String sessionId, int frameIndex, String exposureId,
                                          ExposureData exposureData) implements HandledPayload {
    public static final ResourceLocation ID = ExposureCamcorder.resource("dynamic_capture_frame_data");

    public static DynamicCaptureFrameDataC2SP read(FriendlyByteBuf buffer) {
        return new DynamicCaptureFrameDataC2SP(buffer.readUtf(128), buffer.readVarInt(), buffer.readUtf(128),
                ExposureData.fromPacket(buffer));
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(sessionId, 128);
        buffer.writeVarInt(frameIndex);
        buffer.writeUtf(exposureId, 128);
        exposureData.toPacket(buffer);
    }

    @Override
    public boolean handle(PacketFlow flow, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        return ExposureAccess.receiveFrameData(serverPlayer, sessionId, frameIndex, exposureId, exposureData);
    }
}
