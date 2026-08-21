package io.github.exposure_camcorder.fabric;

import io.github.exposure_camcorder.PlatformHelper;
import net.fabricmc.loader.api.FabricLoader;

public class PlatformHelperImpl {
    public static boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }
}
