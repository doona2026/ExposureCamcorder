package io.github.exposure_camcorder.compatibility.exposure;

import io.github.exposure_camcorder.ExposureCamcorder;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.saveddata.SavedData;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Map;

public final class ExposureStorageAccess {
    private static final String EXPOSURES_DIRECTORY_NAME = "exposures";
    private static final String SAVE_DATA_PREFIX = EXPOSURES_DIRECTORY_NAME + "/";
    private static final Field CACHE_FIELD;

    static {
        try {
            CACHE_FIELD = resolveCacheField();
            CACHE_FIELD.setAccessible(true);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private ExposureStorageAccess() {
    }

    public static void flushDynamicExposure(ServerPlayer player, String exposureId) {
        try {
            MinecraftServer server = player.server;
            DimensionDataStorage dataStorage = server.overworld().getDataStorage();
            String saveDataName = SAVE_DATA_PREFIX + exposureId;
            SavedData savedData = getCache(dataStorage).get(saveDataName);
            if (savedData == null) {
                return;
            }

            HolderLookup.Provider registries = server.overworld().registryAccess();
            savedData.save(resolveExposureFile(server, exposureId), registries);
            getCache(dataStorage).remove(saveDataName, savedData);
        } catch (Exception e) {
            ExposureCamcorder.LOGGER.warn("Failed to flush dynamic exposure '{}' from cache: {}",
                    exposureId, e.toString());
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, SavedData> getCache(DimensionDataStorage dataStorage) throws IllegalAccessException {
        return (Map<String, SavedData>) CACHE_FIELD.get(dataStorage);
    }

    private static Field resolveCacheField() throws NoSuchFieldException {
        for (Field field : DimensionDataStorage.class.getDeclaredFields()) {
            if (Map.class.isAssignableFrom(field.getType())) {
                return field;
            }
        }
        throw new NoSuchFieldException("No Map field found on DimensionDataStorage");
    }

    private static File resolveExposureFile(MinecraftServer server, String exposureId) {
        Path exposuresDirectory = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(EXPOSURES_DIRECTORY_NAME);
        exposuresDirectory.toFile().mkdirs();
        return exposuresDirectory.resolve(exposureId + ".dat").toFile();
    }
}
