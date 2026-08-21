package io.github.exposure_camcorder.network.packet.c2s;

import io.github.exposure_camcorder.ExposureCamcorder;
import io.github.exposure_camcorder.compatibility.exposure.ExposureCameraHooks;
import io.github.exposure_camcorder.network.packet.HandledPayload;
import io.github.exposure_camcorder.world.session.DynamicCaptureSessionEndReason;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class DynamicCameraModeToggleC2SP implements HandledPayload {
    public static final DynamicCameraModeToggleC2SP INSTANCE = new DynamicCameraModeToggleC2SP();
    public static final ResourceLocation ID = ExposureCamcorder.resource("dynamic_camera_mode_toggle");

    private DynamicCameraModeToggleC2SP() {
    }

    public static DynamicCameraModeToggleC2SP read(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
    }

    @Override
    public boolean handle(PacketFlow flow, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        if (ExposureCamcorder.captureSessionManager().getActiveSession(serverPlayer.getUUID()).isPresent()) {
            ExposureCamcorder.captureSessionManager().requestStop(serverPlayer.getUUID(),
                    DynamicCaptureSessionEndReason.RELEASED);
            return true;
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
