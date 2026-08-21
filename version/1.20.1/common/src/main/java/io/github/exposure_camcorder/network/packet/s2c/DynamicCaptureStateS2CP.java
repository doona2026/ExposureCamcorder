package io.github.exposure_camcorder.network.packet.s2c;

import io.github.exposure_camcorder.ExposureCamcorder;
import io.github.exposure_camcorder.client.capture.DynamicFrameCaptureClient;
import io.github.exposure_camcorder.network.packet.HandledPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public record DynamicCaptureStateS2CP(String sessionId, int recordedFrames, boolean stopping) implements HandledPayload {
    public static final ResourceLocation ID = ExposureCamcorder.resource("dynamic_capture_state");

    public static DynamicCaptureStateS2CP read(FriendlyByteBuf buffer) {
        return new DynamicCaptureStateS2CP(buffer.readUtf(128), buffer.readVarInt(), buffer.readBoolean());
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(sessionId, 128);
        buffer.writeVarInt(recordedFrames);
        buffer.writeBoolean(stopping);
    }

    @Override
    public boolean handle(PacketFlow flow, Player player) {
        DynamicFrameCaptureClient.updateState(this);
        return true;
    }
}
