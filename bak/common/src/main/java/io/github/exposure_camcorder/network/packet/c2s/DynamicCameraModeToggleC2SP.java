package io.github.exposure_camcorder.network.packet.c2s;

import io.github.exposure_camcorder.ExposureCamcorder;
import io.github.exposure_camcorder.compatibility.exposure.ExposureCameraHooks;
import io.github.exposure_camcorder.network.packet.HandledPayload;
import io.github.mortuusars.exposure.network.packet.Packet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public final class DynamicCameraModeToggleC2SP implements Packet, HandledPayload, CustomPacketPayload {
    public static final DynamicCameraModeToggleC2SP INSTANCE = new DynamicCameraModeToggleC2SP();
    public static final ResourceLocation ID = ExposureCamcorder.resource("dynamic_camera_mode_toggle");
    public static final Type<DynamicCameraModeToggleC2SP> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, DynamicCameraModeToggleC2SP> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    private DynamicCameraModeToggleC2SP() {
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public boolean handle(PacketFlow flow, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        if (ExposureCamcorder.captureSessionManager().getActiveSession(serverPlayer.getUUID()).isPresent()) {
            return false;
        }

        return ExposureCameraHooks.findModeControlledCamera(serverPlayer)
                .map(stack -> {
                    ExposureCameraHooks.displayModeMessage(serverPlayer,
                            ExposureCameraHooks.toggleMode(stack, serverPlayer));
                    return true;
                })
                .orElse(false);
    }
}
