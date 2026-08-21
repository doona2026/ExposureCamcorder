package io.github.exposure_camcorder.network.packet.s2c;

import io.github.exposure_camcorder.ExposureCamcorder;
import io.github.exposure_camcorder.client.capture.DynamicFrameCaptureClient;
import io.github.exposure_camcorder.network.packet.HandledPayload;
import io.github.mortuusars.exposure.world.camera.capture.CaptureParameters;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public record DynamicCaptureFrameRequestS2CP(String sessionId, int frameIndex, String exposureId,
                                             CaptureParameters captureParameters) implements HandledPayload {
    public static final ResourceLocation ID = ExposureCamcorder.resource("dynamic_capture_frame_request");

    public static DynamicCaptureFrameRequestS2CP read(FriendlyByteBuf buffer) {
        return new DynamicCaptureFrameRequestS2CP(buffer.readUtf(128), buffer.readVarInt(),
                buffer.readUtf(128), CaptureParameters.fromPacket(buffer));
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
        captureParameters.toPacket(buffer);
    }

    @Override
    public boolean handle(PacketFlow flow, Player player) {
        DynamicFrameCaptureClient.captureFrame(this);
        return true;
    }
}
