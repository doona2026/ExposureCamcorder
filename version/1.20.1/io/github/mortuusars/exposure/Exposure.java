package io.github.mortuusars.exposure;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import io.github.mortuusars.exposure.advancements.predicate.TamedPredicate;
import io.github.mortuusars.exposure.advancements.trigger.FrameExposedTrigger;
import io.github.mortuusars.exposure.advancements.trigger.FramePrintedTrigger;
import io.github.mortuusars.exposure.commands.argument.*;
import io.github.mortuusars.exposure.data.ColorPalette;
import io.github.mortuusars.exposure.data.Filter;
import io.github.mortuusars.exposure.data.Lens;
import io.github.mortuusars.exposure.util.supporter.Supporters;
import io.github.mortuusars.exposure.world.block.FlashBlock;
import io.github.mortuusars.exposure.world.block.LightroomBlock;
import io.github.mortuusars.exposure.world.block.entity.LightroomBlockEntity;
import io.github.mortuusars.exposure.world.camera.CameraId;
import io.github.mortuusars.exposure.world.camera.ExposureType;
import io.github.mortuusars.exposure.world.camera.capture.DitherMode;
import io.github.mortuusars.exposure.world.camera.component.CompositionGuide;
import io.github.mortuusars.exposure.world.camera.component.FlashMode;
import io.github.mortuusars.exposure.world.camera.component.SelfTimer;
import io.github.mortuusars.exposure.world.camera.component.ShutterSpeed;
import io.github.mortuusars.exposure.world.camera.film.properties.FilmStyle;
import io.github.mortuusars.exposure.world.camera.frame.Frame;
import io.github.mortuusars.exposure.world.entity.CameraStandEntity;
import io.github.mortuusars.exposure.world.entity.GlassPhotographFrameEntity;
import io.github.mortuusars.exposure.world.entity.PhotographFrameEntity;
import io.github.mortuusars.exposure.world.inventory.*;
import io.github.mortuusars.exposure.world.item.*;
import io.github.mortuusars.exposure.world.item.camera.CameraItem;
import io.github.mortuusars.exposure.world.item.camera.ShutterState;
import io.github.mortuusars.exposure.world.item.component.StoredItemStack;
import io.github.mortuusars.exposure.world.item.component.album.AlbumContent;
import io.github.mortuusars.exposure.world.item.component.album.SignedAlbumContent;
import io.github.mortuusars.exposure.world.item.crafting.recipe.serializer.ComponentTransferringRecipeSerializer;
import io.github.mortuusars.exposure.world.item.interfaces.DefaultFilmStyle;
import io.github.mortuusars.exposure.world.item.util.ItemAndStack;
import io.github.mortuusars.exposure.world.level.storage.ExposureIdentifier;
import net.minecraft.class_1299;
import net.minecraft.class_1311;
import net.minecraft.class_174;
import net.minecraft.class_1747;
import net.minecraft.class_1761;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_2135;
import net.minecraft.class_2248;
import net.minecraft.class_2314;
import net.minecraft.class_2319;
import net.minecraft.class_2378;
import net.minecraft.class_2487;
import net.minecraft.class_2498;
import net.minecraft.class_2499;
import net.minecraft.class_2509;
import net.minecraft.class_2520;
import net.minecraft.class_2561;
import net.minecraft.class_2591;
import net.minecraft.class_2960;
import net.minecraft.class_3414;
import net.minecraft.class_3446;
import net.minecraft.class_3620;
import net.minecraft.class_3917;
import net.minecraft.class_4970;
import net.minecraft.class_5321;
import net.minecraft.class_6862;
import net.minecraft.class_7376;
import net.minecraft.class_7923;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;


public class Exposure {
    public static final String ID = "exposure";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final List<String> MODS_REQUIRING_DIRECT_CAPTURE = List.of("veil", "pmweather");
    public static final int MAX_ENTITIES_IN_FRAME = 10;

    public static void init() {
        Blocks.init();
        BlockEntityTypes.init();
        EntityTypes.init();
        Items.init();
        CreativeTabs.init();
        DataComponents.init();
        ExposureCriteriaTriggers.init();
        ItemSubPredicates.init();
        EntitySubPredicates.init();
        MenuTypes.init();
        RecipeSerializers.init();
        SoundEvents.init();
        ArgumentTypes.init();

        // Query supporters early, so it will be available right away when needed
        Supporters.query();
    }

    public static void initServer(MinecraftServer server) {
        ExposureServer.init(server);
    }

    /**
     * Creates resource location in the mod namespace with the given filePath.
     */
    public static class_2960 resource(String path) {
        return new class_2960(ID, path);
    }

    public static class Blocks {
        public static final Supplier<LightroomBlock> LIGHTROOM = Register.block("lightroom",
              () -> new LightroomBlock(class_4970.class_2251.method_9637()
                    .method_31710(class_3620.field_15977)
                    .method_9632(2.5f)
                    .method_9626(class_2498.field_11547)));

        public static final Supplier<FlashBlock> FLASH = Register.block("flash",
              () -> new FlashBlock(class_4970.class_2251.method_9630(net.minecraft.class_2246.field_10124)
                    .method_9629(-1.0F, 3600000.8F)
                    .method_42327()
                    .method_31710(class_3620.field_16008)
                    .method_22488()
                    .method_9634()
                    .method_9631(state -> 15)));

        static void init() {
        }
    }

    public static class BlockEntityTypes {
        public static final Supplier<class_2591<LightroomBlockEntity>> LIGHTROOM =
              Register.blockEntityType("lightroom", () -> Register.newBlockEntityType(LightroomBlockEntity::new, Blocks.LIGHTROOM.get()));

        static void init() {
        }
    }

    public static class Items {
        public static final Supplier<CameraItem> CAMERA = Register.item("camera",
              () -> new CameraItem(new class_1792.class_1793()
                    .method_7889(1)
                    //       .component(DataComponents.CAMERA_ACTIVE, false)
              ));

        public static final Supplier<FilmRollItem> BLACK_AND_WHITE_FILM = Register.item("black_and_white_film",
              () -> new FilmRollItem(ExposureType.BLACK_AND_WHITE, FilmRollItem.BAR_BLACK_AND_WHITE,
                    new class_1792.class_1793()
                          .method_7889(16), FilmStyle.EMPTY));

        public static final Supplier<FilmRollItem> COLOR_FILM = Register.item("color_film",
              () -> new FilmRollItem(ExposureType.COLOR, FilmRollItem.BAR_COLOR,
                    new class_1792.class_1793()
                          .method_7889(16), FilmStyle.EMPTY));

        public static final Supplier<FilmRollItem> HIGH_SENSITIVITY_BLACK_AND_WHITE_FILM = Register.item("high_sensitivity_black_and_white_film",
              () -> new FilmRollItem(ExposureType.BLACK_AND_WHITE, FilmRollItem.BAR_BLACK_AND_WHITE,
                    new class_1792.class_1793()
                          .method_7889(16), FilmStyle.create()
                    .withSensitivity(2f)
                    .withNoise(0.065f)));

        public static final Supplier<FilmRollItem> HIGH_SENSITIVITY_COLOR_FILM = Register.item("high_sensitivity_color_film",
              () -> new FilmRollItem(ExposureType.COLOR, FilmRollItem.BAR_COLOR,
                    new class_1792.class_1793()
                          .method_7889(16), FilmStyle.create()
                    .withSensitivity(2f)
                    .withNoise(0.065f)));

        public static final Supplier<DevelopedFilmItem> DEVELOPED_BLACK_AND_WHITE_FILM = Register.item("developed_black_and_white_film",
              () -> new DevelopedFilmItem(ExposureType.BLACK_AND_WHITE, new class_1792.class_1793()
                    .method_7889(1)));

        public static final Supplier<DevelopedFilmItem> DEVELOPED_COLOR_FILM = Register.item("developed_color_film",
              () -> new DevelopedFilmItem(ExposureType.COLOR, new class_1792.class_1793()
                    .method_7889(1)));

        public static final Supplier<PhotographItem> PHOTOGRAPH = Register.item("photograph",
              () -> new PhotographItem(new class_1792.class_1793()
                    .method_7889(1)));

        public static final Supplier<ChromaticSheetItem> CHROMATIC_SHEET = Register.item("chromatic_sheet",
              () -> new ChromaticSheetItem(new class_1792.class_1793()
                    .method_7889(1)));

        public static final Supplier<PhotographItem> AGED_PHOTOGRAPH = Register.item("aged_photograph",
              () -> new AgedPhotographItem(new class_1792.class_1793()
                    .method_7889(1)));

        public static final Supplier<InterplanarProjectorItem> INTERPLANAR_PROJECTOR = Register.item("interplanar_projector",
              () -> new InterplanarProjectorItem(new class_1792.class_1793()));
        public static final Supplier<BrokenInterplanarProjectorItem> BROKEN_INTERPLANAR_PROJECTOR = Register.item("broken_interplanar_projector",
              () -> new BrokenInterplanarProjectorItem(new class_1792.class_1793()));

        public static final Supplier<StackedPhotographsItem> STACKED_PHOTOGRAPHS = Register.item("stacked_photographs",
              () -> new StackedPhotographsItem(new class_1792.class_1793()
                    .method_7889(1)));

        public static final Supplier<AlbumItem> ALBUM = Register.item("album",
              () -> new AlbumItem(new class_1792.class_1793()
                    .method_7889(1)));
        public static final Supplier<SignedAlbumItem> SIGNED_ALBUM = Register.item("signed_album",
              () -> new SignedAlbumItem(new class_1792.class_1793()
                    .method_7889(1)));

        public static final Supplier<PhotographFrameItem> PHOTOGRAPH_FRAME = Register.item("photograph_frame",
              () -> new PhotographFrameItem(new class_1792.class_1793()));
        public static final Supplier<GlassPhotographFrameItem> GLASS_PHOTOGRAPH_FRAME = Register.item("glass_photograph_frame",
              () -> new GlassPhotographFrameItem(new class_1792.class_1793()));

        public static final Supplier<CameraStandItem> CAMERA_STAND = Register.item("camera_stand",
              () -> new CameraStandItem(new class_1792.class_1793()));

        public static final Supplier<class_1747> LIGHTROOM = Register.item("lightroom",
              () -> new class_1747(Blocks.LIGHTROOM.get(), new class_1792.class_1793()));

        static void init() {
        }
    }

    public static class CreativeTabs {
        public static final Supplier<class_1761> EXPOSURE = Register.creativeTab("exposure", () ->
              class_1761.method_47307(class_1761.class_7915.field_41049, 0)
                    .method_47321(class_2561.method_43471("itemGroup.exposure.exposure"))
                    .method_47320(() -> new class_1799(Items.CAMERA.get()))
                    .method_47317((params, output) -> {
                        output.method_45421(Items.CAMERA.get());
                        output.method_45421(Items.CAMERA_STAND.get());
                        output.method_45421(Items.BLACK_AND_WHITE_FILM.get());
                        output.method_45421(Items.COLOR_FILM.get());
                        output.method_45421(Items.HIGH_SENSITIVITY_BLACK_AND_WHITE_FILM.get());
                        output.method_45421(Items.HIGH_SENSITIVITY_COLOR_FILM.get());
                        output.method_45421(Items.DEVELOPED_BLACK_AND_WHITE_FILM.get());
                        output.method_45421(Items.DEVELOPED_COLOR_FILM.get());
                        output.method_45421(Items.PHOTOGRAPH.get());
                        output.method_45421(Items.AGED_PHOTOGRAPH.get());
                        output.method_45421(Items.STACKED_PHOTOGRAPHS.get());
                        output.method_45421(Items.ALBUM.get());
                        output.method_45421(Items.PHOTOGRAPH_FRAME.get());
                        output.method_45421(Items.GLASS_PHOTOGRAPH_FRAME.get());
                        output.method_45421(Items.INTERPLANAR_PROJECTOR.get());
                        output.method_45421(Items.LIGHTROOM.get());
                    })
                    .method_47324());

        static void init() {
        }
    }

    public static class DataComponents {
        // Camera State

        public static Boolean getBoolean(class_1799 stack, String key) {//booleans are bytes internally
            return stack.method_7985() && stack.method_7969().method_10573(key, class_2520.field_33251) ? stack.method_7969().method_10577(key) : null;
        }

        public static void setBoolean(class_1799 stack, String key, Boolean value) {
            if (value == null) {
                stack.method_7983(key);
            } else {
                stack.method_7948().method_10556(key, value);
            }
        }

        static Integer getInt(class_1799 stack, String key) {
            return stack.method_7985() && stack.method_7969().method_10573(key, class_2520.field_33253) ? stack.method_7969().method_10550(key) : null;
        }

        static void setInt(class_1799 stack, String key, Integer value) {
            if (value == null) {
                stack.method_7983(key);
            } else {
                stack.method_7948().method_10569(key, value);
            }
        }

        static Long getLong(class_1799 stack, String key) {
            return stack.method_7985() && stack.method_7969().method_10573(key, class_2520.field_33254) ? stack.method_7969().method_10537(key) : null;
        }

        static void setLong(class_1799 stack, String key, Long value) {
            if (value == null) {
                stack.method_7983(key);
            } else {
                stack.method_7948().method_10544(key, value);
            }
        }

        public static Float getFloat(class_1799 stack, String key) {
            return stack.method_7985() && stack.method_7969().method_10573(key, class_2520.field_33255) ? stack.method_7969().method_10583(key) : null;
        }

        public static void setFloat(class_1799 stack, String key, Float value) {
            if (value == null) {
                stack.method_7983(key);
            } else {
                stack.method_7948().method_10548(key, value);
            }
        }

        public static Double getDouble(class_1799 stack, String key) {
            return stack.method_7985() && stack.method_7969().method_10573(key, class_2520.field_33256) ? stack.method_7969().method_10574(key) : null;
        }

        public static void setDouble(class_1799 stack, String key, Double value) {
            if (value == null) {
                stack.method_7983(key);
            } else {
                stack.method_7948().method_10549(key, value);
            }
        }

        static String getString(class_1799 stack, String key) {
            return stack.method_7985() && stack.method_7969().method_10573(key, class_2520.field_33258) ? stack.method_7969().method_10558(key) : null;
        }

        static void setString(class_1799 stack, String key, String value) {
            if (value == null) {
                stack.method_7983(key);
            } else {
                stack.method_7948().method_10582(key, value);
            }
        }


        @Nullable
        static <T> T getValue(class_1799 stack, String key, Codec<T> codec) {
            if (!stack.method_7985()) return null;
            class_2520 tag = stack.method_7969().method_10580(key);
            if (tag == null) return null;
            return codec.parse(new Dynamic<>(class_2509.field_11560, tag)).resultOrPartial(LOGGER::error).orElse(null);
        }

        static <T> void setValue(class_1799 stack, String key, T value, Codec<T> codec) {
            if (value == null) {
                stack.method_7983(key);
            } else {
                codec.encodeStart(class_2509.field_11560, value).resultOrPartial(LOGGER::error).ifPresent(tag -> stack.method_7948().method_10566(key, tag));
            }
        }

        static <E extends Enum<E>> E getEnum(class_1799 stack, String key, Class<E> clazz) {
            E[] constants = clazz.getEnumConstants();
            return stack.method_7985() && stack.method_7969().method_10573(key, class_2520.field_33253) ? constants[stack.method_7969().method_10550(key)] : null;
        }

        static <E extends Enum<E>> void setEnum(class_1799 stack, E value, String key) {
            if (value == null) {
                stack.method_7983(key);
            } else {
                stack.method_7948().method_10569(key, value.ordinal());
            }
        }

        @Nullable
        public static CameraId getCameraId(class_1799 stack) {
            return getValue(stack, "camera_id", CameraId.CODEC);
        }

        public static void setCameraId(class_1799 stack, CameraId id) {
            setValue(stack, "camera_id", id, CameraId.CODEC);
        }

        public static Boolean getCameraGold(class_1799 stack) {
            return getBoolean(stack, "camera_gold");
        }

        public static boolean getCameraGold(class_1799 stack, boolean fallback) {
            Boolean aBoolean = getBoolean(stack, "camera_gold");
            return aBoolean == null ? fallback : aBoolean;
        }

        public static void setCameraGold(class_1799 stack, Boolean cameraGold) {
            setBoolean(stack, "camera_gold", cameraGold);
        }


        public static Boolean getCameraActive(class_1799 stack) {
            return getBoolean(stack, "camera_active");
        }

        public static boolean getCameraActive(class_1799 stack, boolean fallback) {
            Boolean aBoolean = getBoolean(stack, "camera_active");
            return aBoolean == null ? fallback : aBoolean;
        }

        public static void setCameraActive(class_1799 stack, Boolean cameraActive) {
            setBoolean(stack, "camera_active", cameraActive);
        }

        public static Boolean getCameraDisassembled(class_1799 stack) {
            return getBoolean(stack, "camera_disassembled");
        }

        public static boolean getCameraDisassembled(class_1799 stack, boolean fallback) {
            Boolean aBoolean = getBoolean(stack, "camera_disassembled");
            return aBoolean == null ? fallback : aBoolean;
        }

        public static void setCameraDisassembled(class_1799 stack, Boolean cameraDisassembled) {
            setBoolean(stack, "camera_disassembled", cameraDisassembled);
        }

        public static Long getLastCameraActionTime(class_1799 stack) {
            return getLong(stack, "camera_last_action_time");
        }

        public static long getLastCameraActionTime(class_1799 stack, long fallback) {
            Long cameraLastActionTime = getLong(stack, "camera_last_action_time");
            return cameraLastActionTime == null ? fallback : cameraLastActionTime;
        }

        public static void setCameraLastActionTime(class_1799 stack, Long lastCameraActionTime) {
            setLong(stack, "camera_last_action_time", lastCameraActionTime);
        }


        public static Boolean getSelfieMode(class_1799 stack) {
            return getBoolean(stack, "camera_selfie_mode");
        }

        public static boolean getSelfieMode(class_1799 stack, boolean fallback) {
            Boolean aBoolean = getBoolean(stack, "camera_selfie_mode");
            return aBoolean == null ? fallback : aBoolean;
        }

        public static void setSelfieMode(class_1799 stack, Boolean selfieMode) {
            setBoolean(stack, "camera_selfie_mode", selfieMode);
        }

        public static void setCameraShutterState(class_1799 stack, ShutterState state) {
            setValue(stack, "camera_shutter_state", state, ShutterState.CODEC);
        }

        public static ShutterState getCameraShutterState(class_1799 stack) {
            return getValue(stack, "camera_shutter_state", ShutterState.CODEC);
        }

        public static ShutterState getCameraShutterState(class_1799 stack, ShutterState shutterState) {
            ShutterState value = getCameraShutterState(stack);
            return value == null ? shutterState : value;
        }

        public static Long getTimerStartTick(class_1799 stack) {
            return getLong(stack, "camera_timer_start_tick");
        }

        public static long getTimerStartTick(class_1799 stack, long fallback) {
            Long cameraLastActionTime = getLong(stack, "camera_timer_start_tick");
            return cameraLastActionTime == null ? fallback : cameraLastActionTime;
        }

        public static void setTimerStartTick(class_1799 stack, Long timerStartTick) {
            setLong(stack, "camera_timer_start_tick", timerStartTick);
        }


        public static Long getTimerEndTick(class_1799 stack) {
            return getLong(stack, "camera_timer_end_tick");
        }

        public static long getTimerEndTick(class_1799 stack, long fallback) {
            Long cameraLastActionTime = getLong(stack, "camera_timer_end_tick");
            return cameraLastActionTime == null ? fallback : cameraLastActionTime;
        }

        public static void setTimerEndTick(class_1799 stack, Long timerEndTick) {
            setLong(stack, "camera_timer_end_tick", timerEndTick);
        }

        public static Long getTimerLastReleaseTick(class_1799 stack) {
            return getLong(stack, "camera_timer_last_release_tick");
        }

        public static long getTimerLastReleaseTick(class_1799 stack, long fallback) {
            Long cameraLastActionTime = getLong(stack, "camera_timer_last_release_tick");
            return cameraLastActionTime == null ? fallback : cameraLastActionTime;
        }

        public static void setTimerLastReleaseTick(class_1799 stack, Long timerLastReleaseTick) {
            setLong(stack, "camera_timer_last_release_tick", timerLastReleaseTick);
        }

        // Settings

        public static ShutterSpeed getShutterSpeed(class_1799 stack, String key) {
            return getValue(stack, key, ShutterSpeed.CODEC);
        }

        public static void setShutterSpeed(class_1799 stack, String key, ShutterSpeed value) {
            setValue(stack, key, value, ShutterSpeed.CODEC);
        }

        public static CompositionGuide getCompositionGuide(class_1799 stack, String key) {
            return getValue(stack, key, CompositionGuide.CODEC);
        }

        public static void setCompositionGuide(class_1799 stack, String key, CompositionGuide value) {
            setValue(stack, key, value, CompositionGuide.CODEC);
        }

        public static void setSelfTimer(class_1799 stack, String key, SelfTimer selfTimer) {
            setEnum(stack, selfTimer, key);
        }

        public static SelfTimer getSelfTimer(class_1799 stack, String key) {
            return getEnum(stack, key, SelfTimer.class);
        }

        public static void setFlashMode(class_1799 stack, String key, FlashMode flashMode) {
            setEnum(stack, flashMode, key);
        }

        public static FlashMode getFlashMode(class_1799 stack, String key) {
            return getEnum(stack, key, FlashMode.class);
        }

        // Attachments

        public static StoredItemStack getStoredItemStack(class_1799 stack, String key) {
            return getValue(stack, key, StoredItemStack.CODEC);
        }

        public static StoredItemStack getStoredItemStack(class_1799 stack, String key, StoredItemStack fallback) {
            StoredItemStack storedItemStack = getStoredItemStack(stack, key);
            return storedItemStack == null ? fallback : storedItemStack;
        }

        public static void setStoredItemStack(class_1799 stack, String key, StoredItemStack value) {
            setValue(stack, key, value, StoredItemStack.CODEC);
        }

        // Film

        public static Integer getFilmFrameCount(class_1799 stack) {
            return getInt(stack, "film_frame_count");
        }

        public static int getFilmFrameCount(class_1799 stack, int fallback) {
            Integer filmFrameCount = getInt(stack, "film_frame_count");
            return filmFrameCount == null ? fallback : filmFrameCount;
        }


        public static Integer getFilmFrameSize(class_1799 stack) {
            return getInt(stack, "film_frame_size");
        }

        public static void setFilmFrameSize(class_1799 stack, int filmFrameSize) {
            setInt(stack, "film_frame_size", filmFrameSize);
        }

        public static int getFilmFrameSize(class_1799 stack, int fallback) {
            Integer filmFrameSize = getInt(stack, "film_frame_size");
            return filmFrameSize == null ? fallback : filmFrameSize;
        }

        public static void setFilmStyle(class_1799 stack, FilmStyle style) {
            setValue(stack, "film_style", style, FilmStyle.CODEC);
        }

        public static FilmStyle getFilmStyle(class_1799 stack) {
            FilmStyle value = getValue(stack, "film_style", FilmStyle.CODEC);
            if (value == null && stack.method_7909() instanceof DefaultFilmStyle defaultFilmStyle) {
                return defaultFilmStyle.getDefaultFilmStyle();
            }
            return value;
        }

        public static FilmStyle getFilmStyle(class_1799 stack, FilmStyle fallback) {
            FilmStyle filmStyle = getFilmStyle(stack);
            return filmStyle == null ? fallback : filmStyle;
        }

        public static class_2960 getFilmColorPalette(class_1799 stack) {
            return getValue(stack, "film_color_palette", class_2960.field_25139);
        }

        public static DitherMode getFilmDitherMode(class_1799 stack) {
            return getEnum(stack, "film_dither_mode", DitherMode.class);
        }

        public static DitherMode getFilmDitherMode(class_1799 stack, DitherMode fallback) {
            DitherMode filmDitherMode = getFilmDitherMode(stack);
            return filmDitherMode == null ? fallback : filmDitherMode;
        }

        public static void setFilmDitherMode(class_1799 stack, DitherMode mode) {
            setEnum(stack, mode, "film_dither_mode");
        }

        public static void setFilmFrames(class_1799 stack, List<Frame> state) {
            setValue(stack, "film_frames", state, Frame.CODEC.listOf());
        }

        public static List<Frame> getFilmFrames(class_1799 stack) {
            dataFixFilmFrames(stack);
            return getValue(stack, "film_frames", Frame.CODEC.listOf());
        }

        public static List<Frame> getFilmFrames(class_1799 stack, List<Frame> fallback) {
            List<Frame> value = getFilmFrames(stack);
            return value == null ? fallback : value;
        }

        private static void dataFixFilmFrames(class_1799 stack) {
            if (!Config.Common.DATAFIX_OLD_IDS.get()) {
                return;
            }

            @Nullable class_2487 tag = stack.method_7969();
            if (tag != null && !tag.method_33133() && !tag.method_10545("film_frames")) {
                if (tag.method_10573("Frames", class_2520.field_33259)) {
                    class_2499 oldFrames = tag.method_10554("Frames", class_2520.field_33260);
                    class_2499 newFrames = new class_2499();
                    for (int i = 0; i < oldFrames.size(); i++) {
                        class_2487 frame = oldFrames.method_10602(i);
                        if (frame.method_10573("Id", class_2520.field_33258)) {
                            class_2487 newFrame = new class_2487();
                            newFrame.method_10582("identifier", frame.method_10558("Id"));
                            newFrames.add(newFrame);
                        } else if (frame.method_10573("Texture", class_2520.field_33258)) {
                            class_2487 newFrame = new class_2487();
                            class_2487 identifier = new class_2487();
                            identifier.method_10582("texture", frame.method_10558("Texture"));
                            newFrame.method_10566("identifier", identifier);
                            newFrames.add(newFrame);
                        }
                    }

                    tag.method_10566("film_frames", newFrames);
                }
            }
        }

        // Photograph

        private static void dataFixPhotographId(class_1799 stack) {
            if (!Config.Common.DATAFIX_OLD_IDS.get()) {
                return;
            }

            @Nullable class_2487 tag = stack.method_7969();
            if (tag != null && !tag.method_33133() && !tag.method_10545("photograph_frame")) {
                if (tag.method_10573("Id", class_2520.field_33258)) {
                    class_2487 frame = new class_2487();
                    frame.method_10582("identifier", tag.method_10558("Id"));
                    tag.method_10566("photograph_frame", frame);
                } else if (tag.method_10573("Texture", class_2520.field_33258)) {
                    class_2487 frame = new class_2487();
                    class_2487 identifier = new class_2487();
                    identifier.method_10582("texture", tag.method_10558("Texture"));
                    frame.method_10566("identifier", identifier);
                    tag.method_10566("photograph_frame", frame);
                }
            }
        }

        public static @NotNull ExposureIdentifier getExposureIdentifier(class_1799 stack) {
            dataFixPhotographId(stack);

            @Nullable class_2487 tag = stack.method_7969();
            if (tag == null || tag.method_33133()) {
                return ExposureIdentifier.EMPTY;
            }

            @Nullable class_2520 identifierTag = tag.method_10562("photograph_frame").method_10580("identifier");
            if (identifierTag == null) {
                return ExposureIdentifier.EMPTY;
            }

            return ExposureIdentifier.CODEC.parse(class_2509.field_11560, identifierTag)
                  .resultOrPartial(e -> LOGGER.error("Cannot decode ExposureIdentifier from tag '{}': {}", identifierTag, e))
                  .orElse(ExposureIdentifier.EMPTY);
        }

        public static void setPhotographFrame(class_1799 stack, Frame state) {
            setValue(stack, "photograph_frame", state, Frame.CODEC);
        }

        public static Frame getPhotographFrame(class_1799 stack) {
            dataFixPhotographId(stack);
            return getValue(stack, "photograph_frame", Frame.CODEC);
        }

        public static Frame getPhotographFrame(class_1799 stack, Frame fallback) {
            Frame value = getPhotographFrame(stack);
            return value == null ? fallback : value;
        }

        public static void setPhotographType(class_1799 stack, ExposureType state) {
            setValue(stack, "photograph_type", state, ExposureType.CODEC);
        }

        public static ExposureType getPhotographType(class_1799 stack) {
            return getValue(stack, "photograph_type", ExposureType.CODEC);
        }

        public static ExposureType getPhotographType(class_1799 stack, ExposureType fallback) {
            ExposureType value = getPhotographType(stack);
            return value == null ? fallback : value;
        }


        public static Integer getPhotographGeneration(class_1799 stack) {
            return getInt(stack, "photograph_generation");
        }

        public static int getPhotographGeneration(class_1799 stack, int fallback) {
            Integer photographGeneration = getInt(stack, "photograph_generation");
            return photographGeneration == null ? fallback : photographGeneration;
        }

        public static void setPhotographGeneration(class_1799 stack, Integer integer) {
            setInt(stack, "photograph_generation", integer);
        }

        public static List<ItemAndStack<PhotographItem>> getStackedPhotographs(class_1799 stack) {
            return getValue(stack, "stacked_photographs", StackedPhotographsItem.PHOTOGRAPH_ITEM_AND_STACK_CODEC.listOf());
        }

        public static List<ItemAndStack<PhotographItem>> getStackedPhotographs(class_1799 stack, List<ItemAndStack<PhotographItem>> fallback) {
            List<ItemAndStack<PhotographItem>> list = getStackedPhotographs(stack);
            return list == null ? fallback : list;
        }

        public static void setStackedPhotographs(class_1799 stack, List<ItemAndStack<PhotographItem>> value) {
            setValue(stack, "stacked_photographs", value, StackedPhotographsItem.PHOTOGRAPH_ITEM_AND_STACK_CODEC.listOf());
        }

        // Album

        public static AlbumContent getAlbumContent(class_1799 stack) {
            return getValue(stack, "album_content", AlbumContent.CODEC);
        }

        public static AlbumContent getAlbumContent(class_1799 stack, AlbumContent fallback) {
            AlbumContent albumContent = getAlbumContent(stack);
            return albumContent == null ? fallback : albumContent;
        }

        public static void setAlbumContent(class_1799 stack, AlbumContent content) {
            setValue(stack, "album_content", content, AlbumContent.CODEC);
        }

        public static SignedAlbumContent getSignedAlbumContent(class_1799 stack) {
            return getValue(stack, "signed_album_content", SignedAlbumContent.CODEC);
        }

        public static SignedAlbumContent getSignedAlbumContent(class_1799 stack, SignedAlbumContent fallback) {
            SignedAlbumContent signedAlbumContent = getSignedAlbumContent(stack);
            return signedAlbumContent == null ? fallback : signedAlbumContent;
        }

        public static void setSignedAlbumContent(class_1799 stack, SignedAlbumContent content) {
            setValue(stack, "signed_album_content", content, SignedAlbumContent.CODEC);
        }


        // --

        public static DitherMode getInterplanarProjectorMode(class_1799 stack) {
            return getEnum(stack, "interplanar_projector_mode", DitherMode.class);
        }

        public static DitherMode getInterplanarProjectorMode(class_1799 stack, DitherMode fallback) {
            DitherMode interplanarProjectorMode = getInterplanarProjectorMode(stack);
            return interplanarProjectorMode == null ? fallback : interplanarProjectorMode;
        }

        public static void setInterplanarProjectorMode(class_1799 stack, DitherMode mode) {
            setEnum(stack, mode, "interplanar_projector_mode");
        }

        public static String getInterplanarProjectorErrorCode(class_1799 stack, String fallback) {
            String interplanarProjectorErrorCode = getString(stack, "interplanar_projector_error_code");
            return interplanarProjectorErrorCode == null ? fallback : interplanarProjectorErrorCode;
        }

        public static void setInterplanarProjectorErrorCode(class_1799 stack, String code) {
            setString(stack, "interplanar_projector_error_code", code);
        }


        public static void setChromaticLayers(class_1799 stack, List<Frame> state) {
            setValue(stack, "chromatic_layers", state, Frame.CODEC.listOf());
        }

        public static List<Frame> getChromaticLayers(class_1799 stack) {
            return getValue(stack, "chromatic_layers", Frame.CODEC.listOf());
        }

        public static List<Frame> getChromaticLayers(class_1799 stack, List<Frame> fallback) {
            List<Frame> value = getValue(stack, "chromatic_layers", Frame.CODEC.listOf());
            return value == null ? fallback : value;
        }

        static void init() {
        }
    }

    public static class EntityTypes {
        public static final Supplier<class_1299<PhotographFrameEntity>> PHOTOGRAPH_FRAME = Register.entityType("photograph_frame",
              PhotographFrameEntity::new, class_1311.field_17715, false, builder -> builder
                    .method_17687(0.5f, 0.5f)
                    .method_27300(Integer.MAX_VALUE));

        public static final Supplier<class_1299<GlassPhotographFrameEntity>> CLEAR_PHOTOGRAPH_FRAME = Register.entityType("glass_photograph_frame",
              GlassPhotographFrameEntity::new, class_1311.field_17715, false, builder -> builder
                    .method_17687(0.5f, 0.5f)
                    .method_27300(Integer.MAX_VALUE));

        public static final Supplier<class_1299<CameraStandEntity>> CAMERA_STAND = Register.entityType("camera_stand",
              CameraStandEntity::new, class_1311.field_17715, false, builder -> builder
                    .method_17687(0.7f, 1.6f)
                    .method_27300(3)
              //                .eyeHeight(1.40625f)
        );

        static void init() {
        }
    }

    public static class MenuTypes {
        public static final Supplier<class_3917<CameraInHandAttachmentsMenu>> CAMERA_IN_HAND = Register.menuType("camera_in_hand", CameraInHandAttachmentsMenu::fromBuffer);
        public static final Supplier<class_3917<CameraOnStandAttachmentsMenu>> CAMERA_ON_STAND = Register.menuType("camera_on_stand", CameraOnStandAttachmentsMenu::fromBuffer);
        public static final Supplier<class_3917<AlbumMenu>> ALBUM = Register.menuType("album", AlbumMenu::fromBuffer);
        public static final Supplier<class_3917<SignedAlbumMenu>> SIGNED_ALBUM = Register.menuType("signed_album", SignedAlbumMenu::fromBuffer);
        public static final Supplier<class_3917<LecternAlbumMenu>> LECTERN_ALBUM = Register.menuType("lectern_album", LecternAlbumMenu::new);
        public static final Supplier<class_3917<LightroomMenu>> LIGHTROOM = Register.menuType("lightroom", LightroomMenu::fromBuffer);
        public static final Supplier<class_3917<ItemRenameMenu>> ITEM_RENAME = Register.menuType("item_rename", ItemRenameMenu::fromBuffer);

        static void init() {
        }
    }

    public static class RecipeSerializers {
        public static final Supplier<ComponentTransferringRecipeSerializer<?>> COMPONENT_TRANSFERRING =
              Register.recipeSerializer("component_transferring", () ->
                    new ComponentTransferringRecipeSerializer<>("source", ComponentTransferringRecipeSerializer.COMPONENT_TRANSFERRING));

        public static final Supplier<ComponentTransferringRecipeSerializer<?>> FILM_DEVELOPING = Register.recipeSerializer("film_developing",
              () -> new ComponentTransferringRecipeSerializer<>("film", ComponentTransferringRecipeSerializer.FILM_DEVELOPING));
        public static final Supplier<ComponentTransferringRecipeSerializer<?>> PHOTOGRAPH_COPYING = Register.recipeSerializer("photograph_copying",
              () -> new ComponentTransferringRecipeSerializer<>("photograph", ComponentTransferringRecipeSerializer.PHOTOGRAPH_COPYING));
        public static final Supplier<ComponentTransferringRecipeSerializer<?>> PHOTOGRAPH_AGING = Register.recipeSerializer("photograph_aging",
              () -> new ComponentTransferringRecipeSerializer<>("photograph", ComponentTransferringRecipeSerializer.PHOTOGRAPH_AGING));

        static void init() {
        }
    }

    public static class SoundEvents {
        public static final Supplier<class_3414> VIEWFINDER_OPEN = register("item", "camera.viewfinder_open");
        public static final Supplier<class_3414> VIEWFINDER_CLOSE = register("item", "camera.viewfinder_close");
        public static final Supplier<class_3414> SHUTTER_OPEN = register("item", "camera.shutter_open");
        public static final Supplier<class_3414> SHUTTER_CLOSE = register("item", "camera.shutter_close");
        public static final Supplier<class_3414> SHUTTER_TICKING = register("item", "camera.shutter_ticking");
        public static final Supplier<class_3414> FILM_ADVANCE = register("item", "camera.film_advance");
        public static final Supplier<class_3414> FILM_ADVANCE_LAST = register("item", "camera.film_advance_last");
        public static final Supplier<class_3414> FILM_REMOVED = register("item", "camera.film_removed");
        public static final Supplier<class_3414> CAMERA_GENERIC_CLICK = register("item", "camera.generic_click");
        public static final Supplier<class_3414> CAMERA_BUTTON_CLICK = register("item", "camera.button_click");
        public static final Supplier<class_3414> CAMERA_RELEASE_BUTTON_CLICK = register("item", "camera.release_button_click");
        public static final Supplier<class_3414> CAMERA_DIAL_CLICK = register("item", "camera.dial_click");
        public static final Supplier<class_3414> CAMERA_LENS_RING_CLICK = register("item", "camera.lens_ring_click");
        public static final Supplier<class_3414> CAMERA_TIMER_TICK = register("item", "camera.timer_tick");
        public static final Supplier<class_3414> LENS_INSERT = register("item", "camera.lens_insert");
        public static final Supplier<class_3414> LENS_REMOVE = register("item", "camera.lens_remove");
        public static final Supplier<class_3414> FILTER_INSERT = register("item", "camera.filter_insert");
        public static final Supplier<class_3414> FILTER_REMOVE = register("item", "camera.filter_remove");
        public static final Supplier<class_3414> FLASH = register("item", "camera.flash");
        public static final Supplier<class_3414> INTERPLANAR_PROJECT = register("item", "camera.interplanar_projector.project");

        public static final Supplier<class_3414> PHOTOGRAPH_PLACE = register("item", "photograph.place");
        public static final Supplier<class_3414> PHOTOGRAPH_BREAK = register("item", "photograph.break");
        public static final Supplier<class_3414> PHOTOGRAPH_RUSTLE = register("item", "photograph.rustle");

        public static final Supplier<class_3414> PHOTOGRAPH_FRAME_PLACE = register("item", "photograph_frame.place");
        public static final Supplier<class_3414> PHOTOGRAPH_FRAME_BREAK = register("item", "photograph_frame.break");
        public static final Supplier<class_3414> PHOTOGRAPH_FRAME_ADD_ITEM = register("item", "photograph_frame.add_item");
        public static final Supplier<class_3414> PHOTOGRAPH_FRAME_REMOVE_ITEM = register("item", "photograph_frame.remove_item");
        public static final Supplier<class_3414> PHOTOGRAPH_FRAME_ROTATE_ITEM = register("item", "photograph_frame.rotate_item");

        public static final Supplier<class_3414> CAMERA_STAND_PLACE = register("entity", "camera_stand.place");
        public static final Supplier<class_3414> CAMERA_STAND_HIT = register("entity", "camera_stand.hit");
        public static final Supplier<class_3414> CAMERA_STAND_BREAK = register("entity", "camera_stand.break");
        public static final Supplier<class_3414> CAMERA_STAND_SET_CAMERA = register("entity", "camera_stand.set_camera");
        public static final Supplier<class_3414> CAMERA_STAND_REMOVE_CAMERA = register("entity", "camera_stand.remove_camera");

        public static final Supplier<class_3414> LIGHTROOM_PRINT = register("block", "lightroom.print");

        public static final Supplier<class_3414> WRITE = register("misc", "write");
        public static final Supplier<class_3414> BSOD = register("misc", "bsod");

        private static Supplier<class_3414> register(String category, String key) {
            Preconditions.checkState(category != null && !category.isEmpty(), "'category' should not be empty.");
            Preconditions.checkState(key != null && !key.isEmpty(), "'key' should not be empty.");
            String path = category + "." + key;
            return Register.soundEvent(path, () -> class_3414.method_47908(Exposure.resource(path)));
        }

        static void init() {
        }
    }

    public static class Stats {
        public static final Map<class_2960, class_3446> STATS = new HashMap<>();

        public static final class_2960 INTERACT_WITH_LIGHTROOM =
              register(Exposure.resource("interact_with_lightroom"), class_3446.field_16975);
        public static final class_2960 FILM_FRAMES_EXPOSED =
              register(Exposure.resource("film_frames_exposed"), class_3446.field_16975);
        public static final class_2960 FLASHES_TRIGGERED =
              register(Exposure.resource("flashes_triggered"), class_3446.field_16975);

        @SuppressWarnings("SameParameterValue")
        private static class_2960 register(class_2960 location, class_3446 formatter) {
            STATS.put(location, formatter);
            return location;
        }

        public static void register() {
            STATS.forEach((location, formatter) -> {
                net.minecraft.class_2378.method_10230(class_7923.field_41183, location, location);
                net.minecraft.class_3468.field_15419.method_14955(location, formatter);
            });
        }
    }

    public static class ExposureCriteriaTriggers {
        public static FrameExposedTrigger FRAME_EXPOSED = class_174.method_767(new FrameExposedTrigger());
        public static FramePrintedTrigger FRAME_PRINTED = class_174.method_767(new FramePrintedTrigger());
        public static class_2135 PHOTOGRAPH_ENDERMAN_EYES = class_174.method_767(new class_2135(resource("photograph_enderman_eyes")));
        public static class_2135 SUCCESSFULLY_PROJECT_IMAGE = class_174.method_767(new class_2135(resource("successfully_project_image")));

        public static void init() {
        }
    }

    public static class ItemSubPredicates {
        /*     public static Supplier<ItemSubPredicate.NbtType<FramePredicate>> FRAME = Register.itemSubPredicate("frame",
                   () -> new ItemSubPredicate.NbtType<>(FramePredicate.CODEC));
   */
        public static void init() {
        }
    }

    public static class EntitySubPredicates {
        public static final class_7376.class_7377 TAMED = register("tamed", TamedPredicate.CODEC);

        public static void init() {
        }

        public static class_7376.class_7377 register(String name, Codec<? extends class_7376> codec) {
            BiMap<String, class_7376.class_7377> types = HashBiMap.create(class_7376.class_7378.field_38731);
            class_7376.class_7377 type = jsonObject -> codec.decode(JsonOps.INSTANCE, jsonObject).get().orThrow().getFirst();
            types.put(name, type);
            return type;
        }
    }

    public static class LootTables {
        public static final class_2960 SIMPLE_DUNGEON_INJECT = Exposure.resource("chests/simple_dungeon");
        public static final class_2960 ABANDONED_MINESHAFT_INJECT = Exposure.resource("chests/abandoned_mineshaft");
        public static final class_2960 STRONGHOLD_CROSSING_INJECT = Exposure.resource("chests/stronghold_crossing");
        public static final class_2960 VILLAGE_PLAINS_HOUSE_INJECT = Exposure.resource("chests/village_plains_house");
        public static final class_2960 SHIPWRECK_MAP_INJECT = Exposure.resource("chests/shipwreck_map");
    }

    public static class Tags {
        public static class Items {
            public static final class_6862<class_1792> FILM_ROLLS = class_6862.method_40092(net.minecraft.class_7924.field_41197, Exposure.resource("film_rolls"));
            public static final class_6862<class_1792> BLACK_AND_WHITE_FILM_ROLLS = class_6862.method_40092(net.minecraft.class_7924.field_41197, Exposure.resource("black_and_white_film_rolls"));
            public static final class_6862<class_1792> COLOR_FILM_ROLLS = class_6862.method_40092(net.minecraft.class_7924.field_41197, Exposure.resource("color_film_rolls"));
            public static final class_6862<class_1792> DEVELOPED_FILM_ROLLS = class_6862.method_40092(net.minecraft.class_7924.field_41197, Exposure.resource("developed_film_rolls"));
            public static final class_6862<class_1792> CYAN_PRINTING_DYES = class_6862.method_40092(net.minecraft.class_7924.field_41197, Exposure.resource("cyan_printing_dyes"));
            public static final class_6862<class_1792> MAGENTA_PRINTING_DYES = class_6862.method_40092(net.minecraft.class_7924.field_41197, Exposure.resource("magenta_printing_dyes"));
            public static final class_6862<class_1792> YELLOW_PRINTING_DYES = class_6862.method_40092(net.minecraft.class_7924.field_41197, Exposure.resource("yellow_printing_dyes"));
            public static final class_6862<class_1792> BLACK_PRINTING_DYES = class_6862.method_40092(net.minecraft.class_7924.field_41197, Exposure.resource("black_printing_dyes"));
            public static final class_6862<class_1792> PHOTO_PAPERS = class_6862.method_40092(net.minecraft.class_7924.field_41197, Exposure.resource("photo_papers"));
            public static final class_6862<class_1792> PHOTO_AGERS = class_6862.method_40092(net.minecraft.class_7924.field_41197, Exposure.resource("photo_agers"));
            public static final class_6862<class_1792> FLASHES = class_6862.method_40092(net.minecraft.class_7924.field_41197, Exposure.resource("flashes"));
            public static final class_6862<class_1792> LENSES = class_6862.method_40092(net.minecraft.class_7924.field_41197, Exposure.resource("lenses"));
            public static final class_6862<class_1792> FILTERS = class_6862.method_40092(net.minecraft.class_7924.field_41197, Exposure.resource("filters"));

            public static final class_6862<class_1792> RED_FILTERS = class_6862.method_40092(net.minecraft.class_7924.field_41197, Exposure.resource("red_filters"));
            public static final class_6862<class_1792> GREEN_FILTERS = class_6862.method_40092(net.minecraft.class_7924.field_41197, Exposure.resource("green_filters"));
            public static final class_6862<class_1792> BLUE_FILTERS = class_6862.method_40092(net.minecraft.class_7924.field_41197, Exposure.resource("blue_filters"));
        }

        public static class Blocks {
            public static final class_6862<class_2248> CHROMATIC_REFRACTORS = class_6862.method_40092(net.minecraft.class_7924.field_41254, Exposure.resource("chromatic_refractors"));
        }

        public static class Entities {
            public static final class_6862<class_1299<?>> IGNORES_CAMERA = class_6862.method_40092(net.minecraft.class_7924.field_41266, Exposure.resource("ignores_camera"));
        }
    }

    public static class ArgumentTypes {
        public static final Supplier<class_2314<SizeMultiplierArgument, class_2319<SizeMultiplierArgument>.class_7219>> EXPOSURE_SIZE =
              Register.commandArgumentType("exposure_size", SizeMultiplierArgument.class, class_2319.method_41999(SizeMultiplierArgument::new));
        public static final Supplier<class_2314<ExposureLookArgument, class_2319<ExposureLookArgument>.class_7219>> EXPOSURE_LOOK =
              Register.commandArgumentType("exposure_look", ExposureLookArgument.class, class_2319.method_41999(ExposureLookArgument::new));
        public static final Supplier<class_2314<ShaderLocationArgument, class_2319<ShaderLocationArgument>.class_7219>> SHADER_LOCATION =
              Register.commandArgumentType("shader_location", ShaderLocationArgument.class, class_2319.method_41999(ShaderLocationArgument::new));
        public static final Supplier<class_2314<TextureLocationArgument, class_2319<TextureLocationArgument>.class_7219>> TEXTURE_LOCATION =
              Register.commandArgumentType("texture_location", TextureLocationArgument.class, class_2319.method_41999(TextureLocationArgument::new));
        public static final Supplier<class_2314<ColorPaletteArgument, class_2319<ColorPaletteArgument>.class_7219>> COLOR_PALETTE_LOCATION =
              Register.commandArgumentType("color_palette_location", ColorPaletteArgument.class, class_2319.method_41999(ColorPaletteArgument::new));

        public static void init() {
        }
    }

    public static class Registries {
        public static final class_5321<class_2378<ColorPalette>> COLOR_PALETTE = class_5321.method_29180(Exposure.resource("color_palette"));
        public static final class_5321<class_2378<Lens>> LENS = class_5321.method_29180(Exposure.resource("lens"));
        public static final class_5321<class_2378<Filter>> FILTER = class_5321.method_29180(Exposure.resource("filter"));
    }
}
