package io.github.exposure_camcorder.fabric;

import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import io.github.exposure_camcorder.Config;
import io.github.exposure_camcorder.ExposureCamcorder;
import io.github.exposure_camcorder.fabric.network.FabricPackets;
import io.github.exposure_camcorder.world.item.camera.DynamicRecordingTrigger;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraftforge.fml.config.ModConfig;

public class ExposureCamcorderFabric implements ModInitializer {
    private static final DynamicRecordingTrigger RECORDING_TRIGGER = new DynamicRecordingTrigger();

    @Override
    public void onInitialize() {
        ExposureCamcorder.init();
        FabricPackets.registerReceivers();
        ForgeConfigRegistry.INSTANCE.register(ExposureCamcorder.ID, ModConfig.Type.SERVER, Config.Server.SPEC);
        ForgeConfigRegistry.INSTANCE.register(ExposureCamcorder.ID, ModConfig.Type.CLIENT, Config.Client.SPEC);
        ServerTickEvents.END_SERVER_TICK.register(server ->
                server.getPlayerList().getPlayers().forEach(RECORDING_TRIGGER::tickServerPlayer));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                ExposureCamcorder.captureSessionManager().removeSession(handler.player.getUUID()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ExposureCamcorder.captureSessionManager().clear());
    }
}
