package io.github.exposure_camcorder.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;

public interface HandledPayload {
    ResourceLocation getId();

    void write(FriendlyByteBuf buffer);

    boolean handle(PacketFlow flow, Player player);
}
