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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;

@Mod(ExposureCamcorder.ID)
public class ExposureCamcorderNeoForge {
    private static final DynamicRecordingTrigger RECORDING_TRIGGER = new DynamicRecordingTrigger();

    public ExposureCamcorderNeoForge() {
        ExposureCamcorder.init();
        NeoForgePackets.register();

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.Server.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.Client.SPEC);

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        Preconditions.checkNotNull(modEventBus);
        MinecraftForge.EVENT_BUS.addListener(ExposureCamcorderNeoForge::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(ExposureCamcorderNeoForge::onPlayerLoggedOut);
        MinecraftForge.EVENT_BUS.addListener(ExposureCamcorderNeoForge::onServerStopped);

        RegisterImpl.BLOCKS.register(modEventBus);
        RegisterImpl.BLOCK_ENTITY_TYPES.register(modEventBus);
        RegisterImpl.ENTITY_TYPES.register(modEventBus);
        RegisterImpl.ITEMS.register(modEventBus);
        RegisterImpl.CREATIVE_MODE_TABS.register(modEventBus);
        RegisterImpl.MENU_TYPES.register(modEventBus);
        RegisterImpl.RECIPE_TYPES.register(modEventBus);
        RegisterImpl.RECIPE_SERIALIZERS.register(modEventBus);
        RegisterImpl.SOUND_EVENTS.register(modEventBus);
        RegisterImpl.COMMAND_ARGUMENT_TYPES.register(modEventBus);
        RegisterImpl.WORLD_GEN_FEATURES.register(modEventBus);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(DynamicCameraClientInputNeoForge::registerKeyMappings);
            MinecraftForge.EVENT_BUS.addListener(DynamicCameraClientInputNeoForge::onClientTick);
            MinecraftForge.EVENT_BUS.addListener(ExposureCamcorderNeoForge::onClientLoggingOut);
            ExposureCamcorderClient.init();
        }
    }

    private static void onServerTick(ServerTickEvent event) {
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
