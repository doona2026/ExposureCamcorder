package io.github.exposure_camcorder.network.packet.s2c;

import io.github.exposure_camcorder.ExposureCamcorder;
import io.github.exposure_camcorder.network.packet.HandledPayload;
import io.github.exposure_camcorder.client.capture.DynamicFrameCaptureClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public record DynamicCaptureStartS2CP(String sessionId, int captureIntervalTicks, int maxFrames,
                                      int maxRecordingDurationTicks)
        implements HandledPayload {
    public static final ResourceLocation ID = ExposureCamcorder.resource("dynamic_capture_start");

    public static DynamicCaptureStartS2CP read(FriendlyByteBuf buffer) {
        return new DynamicCaptureStartS2CP(buffer.readUtf(128), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt());
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(sessionId, 128);
        buffer.writeVarInt(captureIntervalTicks);
        buffer.writeVarInt(maxFrames);
        buffer.writeVarInt(maxRecordingDurationTicks);
    }

    @Override
    public boolean handle(PacketFlow flow, Player player) {
        DynamicFrameCaptureClient.startSession(this);
        return true;
    }
}
