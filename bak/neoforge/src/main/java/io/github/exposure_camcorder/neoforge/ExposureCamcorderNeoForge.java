package io.github.exposure_camcorder.neoforge;

import com.google.common.base.Preconditions;
import io.github.exposure_camcorder.Config;
import io.github.exposure_camcorder.ExposureCamcorder;
import io.github.exposure_camcorder.ExposureCamcorderClient;
import io.github.exposure_camcorder.client.capture.DynamicFrameCaptureClient;
import io.github.exposure_camcorder.client.playback.DynamicPhotographDisplayPlaybackManager;
import io.github.exposure_camcorder.neoforge.client.DynamicCameraClientInputNeoForge;
import io.github.exposure_camcorder.neoforge.network.NeoForgePackets;
import io.github.exposure_camcorder.world.item.camera.DynamicRecordingTrigger;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(ExposureCamcorder.ID)
public class ExposureCamcorderNeoForge {
    private static final DynamicRecordingTrigger RECORDING_TRIGGER = new DynamicRecordingTrigger();

    public ExposureCamcorderNeoForge(ModContainer container) {
        ExposureCamcorder.init();

        container.registerConfig(ModConfig.Type.SERVER, Config.Server.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, Config.Client.SPEC);

        IEventBus modEventBus = container.getEventBus();
        Preconditions.checkNotNull(modEventBus);
        modEventBus.addListener(NeoForgePackets::register);
        NeoForge.EVENT_BUS.addListener(ExposureCamcorderNeoForge::onServerTick);
        NeoForge.EVENT_BUS.addListener(ExposureCamcorderNeoForge::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(ExposureCamcorderNeoForge::onServerStopped);

        RegisterImpl.BLOCKS.register(modEventBus);
        RegisterImpl.BLOCK_ENTITY_TYPES.register(modEventBus);
        RegisterImpl.ENTITY_TYPES.register(modEventBus);
        RegisterImpl.ITEMS.register(modEventBus);
        RegisterImpl.CREATIVE_MODE_TABS.register(modEventBus);
        RegisterImpl.MENU_TYPES.register(modEventBus);
        RegisterImpl.RECIPE_TYPES.register(modEventBus);
        RegisterImpl.RECIPE_SERIALIZERS.register(modEventBus);
        RegisterImpl.CRITERION_TRIGGERS.register(modEventBus);
        RegisterImpl.ITEM_SUB_PREDICATES.register(modEventBus);
        RegisterImpl.ENTITY_SUB_PREDICATES.register(modEventBus);
        RegisterImpl.SOUND_EVENTS.register(modEventBus);
        RegisterImpl.COMMAND_ARGUMENT_TYPES.register(modEventBus);
        RegisterImpl.WORLD_GEN_FEATURES.register(modEventBus);
        RegisterImpl.DATA_COMPONENT_TYPES.register(modEventBus);
        RegisterImpl.PARTICLE_TYPES.register(modEventBus);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(DynamicCameraClientInputNeoForge::registerKeyMappings);
            NeoForge.EVENT_BUS.addListener(DynamicCameraClientInputNeoForge::onClientTick);
            NeoForge.EVENT_BUS.addListener(ExposureCamcorderNeoForge::onClientLoggingOut);
            ExposureCamcorderClient.init();
        }
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        event.getServer().getPlayerList().getPlayers().forEach(RECORDING_TRIGGER::tickServerPlayer);
    }

    private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ExposureCamcorder.captureSessionManager().removeSession(event.getEntity().getUUID());
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        ExposureCamcorder.captureSessionManager().clear();
    }

    private static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        DynamicFrameCaptureClient.reset();
        DynamicPhotographDisplayPlaybackManager.getInstance().reset();
    }
}
