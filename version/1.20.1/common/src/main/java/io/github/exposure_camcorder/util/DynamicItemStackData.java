package io.github.exposure_camcorder.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import io.github.exposure_camcorder.world.component.DynamicPhotographFrames;
import io.github.exposure_camcorder.world.component.DynamicPhotographSettings;
import io.github.exposure_camcorder.world.component.DynamicPhotographSummary;
import io.github.exposure_camcorder.world.component.DynamicSessionId;
import io.github.exposure_camcorder.world.item.camera.DynamicCameraModeState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class DynamicItemStackData {
    private static final String ROOT_KEY = "exposure_camcorder";
    private static final String FRAMES_KEY = "dynamic_photograph_frames";
    private static final String SETTINGS_KEY = "dynamic_photograph_settings";
    private static final String SUMMARY_KEY = "dynamic_photograph_summary";
    private static final String SESSION_ID_KEY = "dynamic_photograph_session_id";
    private static final String CAMERA_STATE_KEY = "dynamic_camera_mode_state";
    private static final String FILM_MAX_FRAMES_KEY = "dynamic_film_max_frames";
    private static final String FILM_USED_FRAMES_KEY = "dynamic_film_used_frames";

    private DynamicItemStackData() {
    }

    public static DynamicPhotographFrames getDynamicPhotographFrames(ItemStack stack, DynamicPhotographFrames fallback) {
        return read(stack, FRAMES_KEY, DynamicPhotographFrames.CODEC, fallback);
    }

    public static void setDynamicPhotographFrames(ItemStack stack, DynamicPhotographFrames value) {
        write(stack, FRAMES_KEY, DynamicPhotographFrames.CODEC, value);
    }

    public static DynamicPhotographSettings getDynamicPhotographSettings(ItemStack stack, DynamicPhotographSettings fallback) {
        return read(stack, SETTINGS_KEY, DynamicPhotographSettings.CODEC, fallback);
    }

    public static void setDynamicPhotographSettings(ItemStack stack, DynamicPhotographSettings value) {
        write(stack, SETTINGS_KEY, DynamicPhotographSettings.CODEC, value);
    }

    public static DynamicPhotographSummary getDynamicPhotographSummary(ItemStack stack, DynamicPhotographSummary fallback) {
        return read(stack, SUMMARY_KEY, DynamicPhotographSummary.CODEC, fallback);
    }

    public static void setDynamicPhotographSummary(ItemStack stack, DynamicPhotographSummary value) {
        write(stack, SUMMARY_KEY, DynamicPhotographSummary.CODEC, value);
    }

    public static DynamicSessionId getDynamicPhotographSessionId(ItemStack stack, DynamicSessionId fallback) {
        return read(stack, SESSION_ID_KEY, DynamicSessionId.CODEC, fallback);
    }

    public static void setDynamicPhotographSessionId(ItemStack stack, DynamicSessionId value) {
        write(stack, SESSION_ID_KEY, DynamicSessionId.CODEC, value);
    }

    public static DynamicCameraModeState getDynamicCameraModeState(ItemStack stack, DynamicCameraModeState fallback) {
        return read(stack, CAMERA_STATE_KEY, DynamicCameraModeState.CODEC, fallback);
    }

    public static void setDynamicCameraModeState(ItemStack stack, DynamicCameraModeState value) {
        write(stack, CAMERA_STATE_KEY, DynamicCameraModeState.CODEC, value);
    }

    public static int getDynamicFilmMaxFrames(ItemStack stack, int fallback) {
        return readInt(stack, FILM_MAX_FRAMES_KEY, fallback);
    }

    public static void setDynamicFilmMaxFrames(ItemStack stack, int value) {
        writeInt(stack, FILM_MAX_FRAMES_KEY, value);
    }

    public static int getDynamicFilmUsedFrames(ItemStack stack, int fallback) {
        return readInt(stack, FILM_USED_FRAMES_KEY, fallback);
    }

    public static void setDynamicFilmUsedFrames(ItemStack stack, int value) {
        writeInt(stack, FILM_USED_FRAMES_KEY, value);
    }

    private static int readInt(ItemStack stack, String key, int fallback) {
        CompoundTag root = stack.getTagElement(ROOT_KEY);
        return root != null && root.contains(key) ? root.getInt(key) : fallback;
    }

    private static void writeInt(ItemStack stack, String key, int value) {
        stack.getOrCreateTagElement(ROOT_KEY).putInt(key, value);
    }

    private static <T> T read(ItemStack stack, String key, Codec<T> codec, T fallback) {
        CompoundTag root = stack.getTagElement(ROOT_KEY);
        if (root == null || !root.contains(key)) {
            return fallback;
        }

        return codec.parse(NbtOps.INSTANCE, root.get(key)).result().orElse(fallback);
    }

    private static <T> void write(ItemStack stack, String key, Codec<T> codec, T value) {
        Optional<?> encoded = codec.encodeStart(NbtOps.INSTANCE, value).result();
        encoded.ifPresent(nbt -> stack.getOrCreateTagElement(ROOT_KEY).put(key, (net.minecraft.nbt.Tag) nbt));
    }
}
