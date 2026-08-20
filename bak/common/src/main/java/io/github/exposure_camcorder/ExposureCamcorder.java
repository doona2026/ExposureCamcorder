package io.github.exposure_camcorder;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;

import io.github.exposure_camcorder.ExposureCamcorder.Items;
import io.github.exposure_camcorder.world.component.DynamicPhotographFrames;
import io.github.exposure_camcorder.world.component.DynamicPhotographSettings;
import io.github.exposure_camcorder.world.component.DynamicPhotographSummary;
import io.github.exposure_camcorder.world.component.DynamicSessionId;
import io.github.exposure_camcorder.world.item.DevelopedDynamicFilmItem;
import io.github.exposure_camcorder.world.item.DynamicFilmItem;
import io.github.exposure_camcorder.world.item.DynamicPhotographItem;
import io.github.exposure_camcorder.world.item.camera.DynamicCameraModeState;
import io.github.mortuusars.exposure.world.camera.ExposureType;
import io.github.exposure_camcorder.world.session.DynamicCaptureSessionManager;
import io.github.exposure_camcorder.world.session.DynamicCaptureTicker;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class ExposureCamcorder {
    public static final String ID = "exposure_camcorder";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final DynamicCaptureSessionManager CAPTURE_SESSION_MANAGER = new DynamicCaptureSessionManager();
    private static final DynamicCaptureTicker CAPTURE_TICKER = new DynamicCaptureTicker();

    public static void init() {
        DataComponents.init();
        Items.init();
        CreativeTabs.init();
    }

    public static ResourceLocation resource(String path) {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }

    public static DynamicCaptureSessionManager captureSessionManager() {
        return CAPTURE_SESSION_MANAGER;
    }

    public static DynamicCaptureTicker captureTicker() {
        return CAPTURE_TICKER;
    }

    public static class Items {
        public static final Supplier<DynamicFilmItem> DYNAMIC_FILM = Register.item("dynamic_film",
                () -> new DynamicFilmItem(new Item.Properties().stacksTo(16)));
        public static final Supplier<DynamicFilmItem> DYNAMIC_BW_FILM = Register.item("dynamic_bw_film",
                () -> new DynamicFilmItem(ExposureType.BLACK_AND_WHITE,
                        io.github.mortuusars.exposure.world.item.FilmRollItem.BAR_BLACK_AND_WHITE,
                        new Item.Properties().stacksTo(16),
                        Config.Server.DEFAULT_DYNAMIC_FILM_MAX_FRAMES::get,
                        io.github.mortuusars.exposure.Config.Server.DEFAULT_FRAME_SIZE::get));
        public static final Supplier<DynamicFilmItem> DYNAMIC_BW_FILM_80 = Register.item("dynamic_bw_film_80",
                () -> new DynamicFilmItem(ExposureType.BLACK_AND_WHITE,
                        io.github.mortuusars.exposure.world.item.FilmRollItem.BAR_BLACK_AND_WHITE,
                        new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON), () -> 80,
                        io.github.mortuusars.exposure.Config.Server.DEFAULT_FRAME_SIZE::get));
        public static final Supplier<DynamicFilmItem> DYNAMIC_BW_FILM_160 = Register.item("dynamic_bw_film_160",
                () -> new DynamicFilmItem(ExposureType.BLACK_AND_WHITE,
                        io.github.mortuusars.exposure.world.item.FilmRollItem.BAR_BLACK_AND_WHITE,
                        new Item.Properties().stacksTo(16).rarity(Rarity.RARE), () -> 160,
                        io.github.mortuusars.exposure.Config.Server.DEFAULT_FRAME_SIZE::get));
        public static final Supplier<DynamicFilmItem> DYNAMIC_BW_FILM_320 = Register.item("dynamic_bw_film_320",
                () -> new DynamicFilmItem(ExposureType.BLACK_AND_WHITE,
                        io.github.mortuusars.exposure.world.item.FilmRollItem.BAR_BLACK_AND_WHITE,
                        new Item.Properties().stacksTo(16).rarity(Rarity.EPIC), () -> 320,
                        io.github.mortuusars.exposure.Config.Server.DEFAULT_FRAME_SIZE::get));
        public static final Supplier<DynamicFilmItem> DYNAMIC_BW_FILM_600 = Register.item("dynamic_bw_film_600",
                () -> new DynamicFilmItem(ExposureType.BLACK_AND_WHITE,
                        io.github.mortuusars.exposure.world.item.FilmRollItem.BAR_BLACK_AND_WHITE,
                        new Item.Properties().stacksTo(16).rarity(Rarity.EPIC), () -> 600,
                        io.github.mortuusars.exposure.Config.Server.DEFAULT_FRAME_SIZE::get));
        public static final Supplier<DynamicFilmItem> DYNAMIC_FILM_80 = Register.item("dynamic_film_80",
                () -> new DynamicFilmItem(ExposureType.COLOR, io.github.mortuusars.exposure.world.item.FilmRollItem.BAR_COLOR,
                        new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON), () -> 80,
                        io.github.mortuusars.exposure.Config.Server.DEFAULT_FRAME_SIZE::get));
        public static final Supplier<DynamicFilmItem> DYNAMIC_FILM_160 = Register.item("dynamic_film_160",
                () -> new DynamicFilmItem(ExposureType.COLOR, io.github.mortuusars.exposure.world.item.FilmRollItem.BAR_COLOR,
                        new Item.Properties().stacksTo(16).rarity(Rarity.RARE), () -> 160,
                        io.github.mortuusars.exposure.Config.Server.DEFAULT_FRAME_SIZE::get));
        public static final Supplier<DynamicFilmItem> DYNAMIC_FILM_320 = Register.item("dynamic_film_320",
                () -> new DynamicFilmItem(ExposureType.COLOR, io.github.mortuusars.exposure.world.item.FilmRollItem.BAR_COLOR,
                        new Item.Properties().stacksTo(16).rarity(Rarity.EPIC), () -> 320,
                        io.github.mortuusars.exposure.Config.Server.DEFAULT_FRAME_SIZE::get));
        public static final Supplier<DynamicFilmItem> DYNAMIC_FILM_600 = Register.item("dynamic_film_600",
                () -> new DynamicFilmItem(ExposureType.COLOR, io.github.mortuusars.exposure.world.item.FilmRollItem.BAR_COLOR,
                        new Item.Properties().stacksTo(16).rarity(Rarity.EPIC), () -> 600,
                        io.github.mortuusars.exposure.Config.Server.DEFAULT_FRAME_SIZE::get));
        public static final Supplier<DynamicFilmItem> DYNAMIC_HIRES_FILM = Register.item("dynamic_hires_film",
                () -> new DynamicFilmItem(ExposureType.COLOR, io.github.mortuusars.exposure.world.item.FilmRollItem.BAR_COLOR,
                        new Item.Properties().stacksTo(16).rarity(Rarity.RARE), () -> 40, () -> 640));
        public static final Supplier<DynamicFilmItem> DYNAMIC_HIRES_FILM_80 = Register.item("dynamic_hires_film_80",
                () -> new DynamicFilmItem(ExposureType.COLOR, io.github.mortuusars.exposure.world.item.FilmRollItem.BAR_COLOR,
                        new Item.Properties().stacksTo(16).rarity(Rarity.RARE), () -> 80, () -> 640));
        public static final Supplier<DynamicFilmItem> DYNAMIC_HIRES_FILM_160 = Register.item("dynamic_hires_film_160",
                () -> new DynamicFilmItem(ExposureType.COLOR, io.github.mortuusars.exposure.world.item.FilmRollItem.BAR_COLOR,
                        new Item.Properties().stacksTo(16).rarity(Rarity.EPIC), () -> 160, () -> 640));
        public static final Supplier<DynamicFilmItem> DYNAMIC_HIRES_FILM_320 = Register.item("dynamic_hires_film_320",
                () -> new DynamicFilmItem(ExposureType.COLOR, io.github.mortuusars.exposure.world.item.FilmRollItem.BAR_COLOR,
                        new Item.Properties().stacksTo(16).rarity(Rarity.EPIC), () -> 320, () -> 640));
        public static final Supplier<DynamicFilmItem> DYNAMIC_HIRES_FILM_600 = Register.item("dynamic_hires_film_600",
                () -> new DynamicFilmItem(ExposureType.COLOR, io.github.mortuusars.exposure.world.item.FilmRollItem.BAR_COLOR,
                        new Item.Properties().stacksTo(16).rarity(Rarity.EPIC), () -> 600, () -> 640));
        public static final Supplier<DynamicFilmItem> DYNAMIC_HIRES_BW_FILM = Register.item("dynamic_hires_bw_film",
                () -> new DynamicFilmItem(ExposureType.BLACK_AND_WHITE,
                        io.github.mortuusars.exposure.world.item.FilmRollItem.BAR_BLACK_AND_WHITE,
                        new Item.Properties().stacksTo(16).rarity(Rarity.RARE), () -> 40, () -> 640));
        public static final Supplier<DynamicFilmItem> DYNAMIC_HIRES_BW_FILM_80 = Register.item("dynamic_hires_bw_film_80",
                () -> new DynamicFilmItem(ExposureType.BLACK_AND_WHITE,
                        io.github.mortuusars.exposure.world.item.FilmRollItem.BAR_BLACK_AND_WHITE,
                        new Item.Properties().stacksTo(16).rarity(Rarity.RARE), () -> 80, () -> 640));
        public static final Supplier<DynamicFilmItem> DYNAMIC_HIRES_BW_FILM_160 = Register.item("dynamic_hires_bw_film_160",
                () -> new DynamicFilmItem(ExposureType.BLACK_AND_WHITE,
                        io.github.mortuusars.exposure.world.item.FilmRollItem.BAR_BLACK_AND_WHITE,
                        new Item.Properties().stacksTo(16).rarity(Rarity.EPIC), () -> 160, () -> 640));
        public static final Supplier<DynamicFilmItem> DYNAMIC_HIRES_BW_FILM_320 = Register.item("dynamic_hires_bw_film_320",
                () -> new DynamicFilmItem(ExposureType.BLACK_AND_WHITE,
                        io.github.mortuusars.exposure.world.item.FilmRollItem.BAR_BLACK_AND_WHITE,
                        new Item.Properties().stacksTo(16).rarity(Rarity.EPIC), () -> 320, () -> 640));
        public static final Supplier<DynamicFilmItem> DYNAMIC_HIRES_BW_FILM_600 = Register.item("dynamic_hires_bw_film_600",
                () -> new DynamicFilmItem(ExposureType.BLACK_AND_WHITE,
                        io.github.mortuusars.exposure.world.item.FilmRollItem.BAR_BLACK_AND_WHITE,
                        new Item.Properties().stacksTo(16).rarity(Rarity.EPIC), () -> 600, () -> 640));
        public static final Supplier<DynamicFilmItem> DYNAMIC_ULTRA_HIRES_FILM = Register.item("dynamic_ultra_hires_film",
                () -> new DynamicFilmItem(ExposureType.COLOR, io.github.mortuusars.exposure.world.item.FilmRollItem.BAR_COLOR,
                        new Item.Properties().stacksTo(16).rarity(Rarity.EPIC), () -> 40, () -> 960));
        public static final Supplier<DynamicFilmItem> DYNAMIC_ULTRA_HIRES_FILM_80 = Register.item("dynamic_ultra_hires_film_80",
                () -> new DynamicFilmItem(ExposureType.COLOR, io.github.mortuusars.exposure.world.item.FilmRollItem.BAR_COLOR,
                        new Item.Properties().stacksTo(16).rarity(Rarity.EPIC), () -> 80, () -> 960));
        public static final Supplier<DynamicFilmItem> DYNAMIC_ULTRA_HIRES_FILM_160 = Register.item("dynamic_ultra_hires_film_160",
                () -> new DynamicFilmItem(ExposureType.COLOR, io.github.mortuusars.exposure.world.item.FilmRollItem.BAR_COLOR,
                        new Item.Properties().stacksTo(16).rarity(Rarity.EPIC), () -> 160, () -> 960));
        public static final Supplier<DynamicFilmItem> DYNAMIC_ULTRA_HIRES_FILM_320 = Register.item("dynamic_ultra_hires_film_320",
                () -> new DynamicFilmItem(ExposureType.COLOR, io.github.mortuusars.exposure.world.item.FilmRollItem.BAR_COLOR,
                        new Item.Properties().stacksTo(16).rarity(Rarity.EPIC), () -> 320, () -> 960));
        public static final Supplier<DynamicFilmItem> DYNAMIC_ULTRA_HIRES_FILM_600 = Register.item("dynamic_ultra_hires_film_600",
                () -> new DynamicFilmItem(ExposureType.COLOR, io.github.mortuusars.exposure.world.item.FilmRollItem.BAR_COLOR,
                        new Item.Properties().stacksTo(16).rarity(Rarity.EPIC), () -> 600, () -> 960));
        public static final Supplier<DynamicFilmItem> DYNAMIC_ULTRA_HIRES_BW_FILM = Register.item("dynamic_ultra_hires_bw_film",
                () -> new DynamicFilmItem(ExposureType.BLACK_AND_WHITE,
                        io.github.mortuusars.exposure.world.item.FilmRollItem.BAR_BLACK_AND_WHITE,
                        new Item.Properties().stacksTo(16).rarity(Rarity.EPIC), () -> 40, () -> 960));
        public static final Supplier<DynamicFilmItem> DYNAMIC_ULTRA_HIRES_BW_FILM_80 = Register.item("dynamic_ultra_hires_bw_film_80",
                () -> new DynamicFilmItem(ExposureType.BLACK_AND_WHITE,
                        io.github.mortuusars.exposure.world.item.FilmRollItem.BAR_BLACK_AND_WHITE,
                        new Item.Properties().stacksTo(16).rarity(Rarity.EPIC), () -> 80, () -> 960));
        public static final Supplier<DynamicFilmItem> DYNAMIC_ULTRA_HIRES_BW_FILM_160 = Register.item("dynamic_ultra_hires_bw_film_160",
                () -> new DynamicFilmItem(ExposureType.BLACK_AND_WHITE,
                        io.github.mortuusars.exposure.world.item.FilmRollItem.BAR_BLACK_AND_WHITE,
                        new Item.Properties().stacksTo(16).rarity(Rarity.EPIC), () -> 160, () -> 960));
        public static final Supplier<DynamicFilmItem> DYNAMIC_ULTRA_HIRES_BW_FILM_320 = Register.item("dynamic_ultra_hires_bw_film_320",
                () -> new DynamicFilmItem(ExposureType.BLACK_AND_WHITE,
                        io.github.mortuusars.exposure.world.item.FilmRollItem.BAR_BLACK_AND_WHITE,
                        new Item.Properties().stacksTo(16).rarity(Rarity.EPIC), () -> 320, () -> 960));
        public static final Supplier<DynamicFilmItem> DYNAMIC_ULTRA_HIRES_BW_FILM_600 = Register.item("dynamic_ultra_hires_bw_film_600",
                () -> new DynamicFilmItem(ExposureType.BLACK_AND_WHITE,
                        io.github.mortuusars.exposure.world.item.FilmRollItem.BAR_BLACK_AND_WHITE,
                        new Item.Properties().stacksTo(16).rarity(Rarity.EPIC), () -> 600, () -> 960));
        public static final Supplier<DevelopedDynamicFilmItem> DEVELOPED_DYNAMIC_FILM = Register.item("developed_dynamic_film",
                () -> new DevelopedDynamicFilmItem(ExposureType.COLOR, new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
        public static final Supplier<DevelopedDynamicFilmItem> DEVELOPED_DYNAMIC_BW_FILM = Register.item("developed_dynamic_bw_film",
                () -> new DevelopedDynamicFilmItem(ExposureType.BLACK_AND_WHITE,
                        new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
        public static final Supplier<DynamicPhotographItem> DYNAMIC_PHOTOGRAPH = Register.item("dynamic_photograph",
                () -> new DynamicPhotographItem(new Item.Properties().stacksTo(1)));

        static void init() {
        }
    }

    public static class CreativeTabs {
        public static final Supplier<CreativeModeTab> MAIN = Register.creativeTab("main", () ->
                CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                        .title(Component.translatable("itemGroup.exposure_camcorder.main"))
                        .icon(() -> new ItemStack(Items.DYNAMIC_FILM.get()))
                        .displayItems((parameters, output) -> {
                            output.accept(Items.DYNAMIC_FILM.get());
                            output.accept(Items.DYNAMIC_BW_FILM.get());
                            output.accept(Items.DYNAMIC_BW_FILM_80.get());
                            output.accept(Items.DYNAMIC_BW_FILM_160.get());
                            output.accept(Items.DYNAMIC_BW_FILM_320.get());
                            output.accept(Items.DYNAMIC_BW_FILM_600.get());
                            output.accept(Items.DYNAMIC_FILM_80.get());
                            output.accept(Items.DYNAMIC_FILM_160.get());
                            output.accept(Items.DYNAMIC_FILM_320.get());
                            output.accept(Items.DYNAMIC_FILM_600.get());
                            output.accept(Items.DYNAMIC_HIRES_FILM.get());
                            output.accept(Items.DYNAMIC_HIRES_FILM_80.get());
                            output.accept(Items.DYNAMIC_HIRES_FILM_160.get());
                            output.accept(Items.DYNAMIC_HIRES_FILM_320.get());
                            output.accept(Items.DYNAMIC_HIRES_FILM_600.get());
                            output.accept(Items.DYNAMIC_HIRES_BW_FILM.get());
                            output.accept(Items.DYNAMIC_HIRES_BW_FILM_80.get());
                            output.accept(Items.DYNAMIC_HIRES_BW_FILM_160.get());
                            output.accept(Items.DYNAMIC_HIRES_BW_FILM_320.get());
                            output.accept(Items.DYNAMIC_HIRES_BW_FILM_600.get());
                            output.accept(Items.DYNAMIC_ULTRA_HIRES_FILM.get());
                            output.accept(Items.DYNAMIC_ULTRA_HIRES_FILM_80.get());
                            output.accept(Items.DYNAMIC_ULTRA_HIRES_FILM_160.get());
                            output.accept(Items.DYNAMIC_ULTRA_HIRES_FILM_320.get());
                            output.accept(Items.DYNAMIC_ULTRA_HIRES_FILM_600.get());
                            output.accept(Items.DYNAMIC_ULTRA_HIRES_BW_FILM.get());
                            output.accept(Items.DYNAMIC_ULTRA_HIRES_BW_FILM_80.get());
                            output.accept(Items.DYNAMIC_ULTRA_HIRES_BW_FILM_160.get());
                            output.accept(Items.DYNAMIC_ULTRA_HIRES_BW_FILM_320.get());
                            output.accept(Items.DYNAMIC_ULTRA_HIRES_BW_FILM_600.get());
                            output.accept(Items.DEVELOPED_DYNAMIC_FILM.get());
                            output.accept(Items.DEVELOPED_DYNAMIC_BW_FILM.get());
                            output.accept(Items.DYNAMIC_PHOTOGRAPH.get());
                        })
                        .build());

        static void init() {
        }
    }

    public static class DataComponents {
        public static final net.minecraft.core.component.DataComponentType<DynamicPhotographFrames> DYNAMIC_PHOTOGRAPH_FRAMES =
                Register.dataComponentType("dynamic_photograph_frames",
                        builder -> builder.persistent(DynamicPhotographFrames.CODEC)
                                .networkSynchronized(DynamicPhotographFrames.STREAM_CODEC));

        public static final net.minecraft.core.component.DataComponentType<DynamicPhotographSettings> DYNAMIC_PHOTOGRAPH_SETTINGS =
                Register.dataComponentType("dynamic_photograph_settings",
                        builder -> builder.persistent(DynamicPhotographSettings.CODEC)
                                .networkSynchronized(DynamicPhotographSettings.STREAM_CODEC));

        public static final net.minecraft.core.component.DataComponentType<DynamicPhotographSummary> DYNAMIC_PHOTOGRAPH_SUMMARY =
                Register.dataComponentType("dynamic_photograph_summary",
                        builder -> builder.persistent(DynamicPhotographSummary.CODEC)
                                .networkSynchronized(DynamicPhotographSummary.STREAM_CODEC));

        public static final net.minecraft.core.component.DataComponentType<DynamicSessionId> DYNAMIC_PHOTOGRAPH_SESSION_ID =
                Register.dataComponentType("dynamic_photograph_session_id",
                        builder -> builder.persistent(DynamicSessionId.CODEC)
                                .networkSynchronized(DynamicSessionId.STREAM_CODEC));

        public static final net.minecraft.core.component.DataComponentType<DynamicCameraModeState> DYNAMIC_CAMERA_MODE_STATE =
                Register.dataComponentType("dynamic_camera_mode_state",
                        builder -> builder.persistent(DynamicCameraModeState.CODEC)
                                .networkSynchronized(DynamicCameraModeState.STREAM_CODEC));

        public static final net.minecraft.core.component.DataComponentType<Integer> DYNAMIC_FILM_MAX_FRAMES =
                Register.dataComponentType("dynamic_film_max_frames",
                        builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

        public static final net.minecraft.core.component.DataComponentType<Integer> DYNAMIC_FILM_USED_FRAMES =
                Register.dataComponentType("dynamic_film_used_frames",
                        builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

        static void init() {
        }
    }
}
