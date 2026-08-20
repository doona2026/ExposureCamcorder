package io.github.exposure_camcorder;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class Config {
    public static class Server {
        public static final ModConfigSpec SPEC;
        public static final ModConfigSpec.IntValue DEFAULT_CAPTURE_INTERVAL_TICKS;
        public static final ModConfigSpec.IntValue MAX_RECORDING_DURATION_TICKS;
        public static final ModConfigSpec.IntValue DEFAULT_DYNAMIC_FILM_MAX_FRAMES;

        static {
            ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

            builder.push("dynamic_photograph");
            DEFAULT_CAPTURE_INTERVAL_TICKS = builder
                    .comment("Default capture interval in ticks per frame.")
                    .defineInRange("default_capture_interval_ticks", 2, 1, 20 * 30);
            MAX_RECORDING_DURATION_TICKS = builder
                    .comment("Maximum recording duration in ticks for a single dynamic photograph.")
                    .defineInRange("max_recording_duration_ticks", 80, 1, 20 * 60);
            DEFAULT_DYNAMIC_FILM_MAX_FRAMES = builder
                    .comment("Maximum amount of frames one Dynamic Film can hold by default.")
                    .defineInRange("default_dynamic_film_max_frames", 40, 1, 600);
            builder.pop();

            SPEC = builder.build();
        }
    }

    public static class Client {
        public static final ModConfigSpec SPEC;
        public static final ModConfigSpec.ConfigValue<List<? extends Integer>> VIEWER_SPEED_OPTIONS;
        public static final ModConfigSpec.BooleanValue SHOW_DETAILED_RECORDING_STATUS;

        static {
            ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

            builder.push("dynamic_photograph");
            VIEWER_SPEED_OPTIONS = builder
                    .comment("Candidate playback speeds in ticks per frame for the viewer UI.")
                    .defineListAllowEmpty("viewer_speed_options", () -> List.of(1, 2, 4, 6, 8), () -> 2,
                            value -> value instanceof Integer intValue && intValue > 0 && intValue <= 40);
            SHOW_DETAILED_RECORDING_STATUS = builder
                    .comment("Shows detailed recording status information in viewfinder UI.")
                    .define("show_detailed_recording_status", true);
            builder.pop();

            SPEC = builder.build();
        }
    }
}
