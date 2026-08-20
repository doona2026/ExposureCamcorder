package io.github.exposure_camcorder.fabric;

import net.fabricmc.loader.api.FabricLoader;

public class PlatformHelperImpl {
    public static boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    public static boolean isInDevEnv() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }
}
