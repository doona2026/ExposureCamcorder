package io.github.exposure_camcorder.neoforge;

import io.github.exposure_camcorder.PlatformHelper;
import net.minecraftforge.fml.ModList;

public class PlatformHelperImpl {
    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }
}
