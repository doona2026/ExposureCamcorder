package io.github.exposure_camcorder.client.export;

import io.github.exposure_camcorder.ExposureCamcorder;
import io.github.exposure_camcorder.world.component.DynamicPhotographFrames;
import io.github.exposure_camcorder.world.component.DynamicSessionId;
import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.client.image.Image;
import io.github.mortuusars.exposure.client.image.PalettedImage;
import io.github.mortuusars.exposure.client.util.Minecrft;
import io.github.mortuusars.exposure.world.camera.frame.Frame;
import io.github.mortuusars.exposure.world.level.storage.ExposureData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DynamicPhotographGifExport {
    static final int REQUEST_ATTEMPTS = 50;
    static final int REQUEST_DELAY_MILLIS = 100;

    private static final AtomicBoolean EXPORT_IN_PROGRESS = new AtomicBoolean(false);

    private DynamicPhotographGifExport() {
    }

    public static boolean export(ItemStack stack, double effectiveTicksPerFrame, boolean loop) {
        if (!EXPORT_IN_PROGRESS.compareAndSet(false, true)) {
            displayMessage(Component.translatable("message.exposure_camcorder.dynamic_photograph.export.already_running"), false);
            return true;
        }

        DynamicPhotographFrames frames = stack.getOrDefault(ExposureCamcorder.DataComponents.DYNAMIC_PHOTOGRAPH_FRAMES,
                DynamicPhotographFrames.EMPTY);
        List<String> exposureIds = collectExposureIds(frames);
        if (exposureIds.isEmpty()) {
            EXPORT_IN_PROGRESS.set(false);
            displayMessage(Component.translatable("message.exposure_camcorder.dynamic_photograph.export.no_frames"), false);
            return true;
        }

        DynamicSessionId sessionId = stack.getOrDefault(ExposureCamcorder.DataComponents.DYNAMIC_PHOTOGRAPH_SESSION_ID,
                DynamicSessionId.EMPTY);
        int frameDelayCentiseconds = resolveFrameDelayCentiseconds(effectiveTicksPerFrame);

        displayMessage(Component.translatable("message.exposure_camcorder.dynamic_photograph.export.started", exposureIds.size()), false);

        CompletableFuture.supplyAsync(() -> exportGif(sessionId, exposureIds, frameDelayCentiseconds, loop))
                .whenComplete((result, throwable) -> {
                    EXPORT_IN_PROGRESS.set(false);
                    if (throwable != null) {
                        ExposureCamcorder.LOGGER.error("Failed to export dynamic photograph gif.", throwable);
                        displayMessage(Component.translatable("message.exposure_camcorder.dynamic_photograph.export.failed",
                                throwable.getMessage()), false);
                        return;
                    }

                    if (!result.exported()) {
                        displayMessage(Component.translatable("message.exposure_camcorder.dynamic_photograph.export.failed",
                                result.errorMessage()), false);
                        return;
                    }

                    MutableComponent message = Component.translatable(
                            "message.exposure_camcorder.dynamic_photograph.export.success",
                            result.exportedFrameCount(),
                            result.totalFrameCount());

                    if (result.outputFile() != null) {
                        message.append(Component.literal(" '" + result.outputFile().getName() + "'")
                                .withStyle(Style.EMPTY
                                        .withColor(ChatFormatting.GREEN)
                                        .withUnderlined(true)
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                Component.translatable("message.exposure_camcorder.dynamic_photograph.export.open_file")))
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE,
                                                result.outputFile().getAbsolutePath()))));
                    }

                    displayMessage(message, false);
                });

        return true;
    }

    static List<String> collectExposureIds(DynamicPhotographFrames frames) {
        ArrayList<String> exposureIds = new ArrayList<>(frames.size());
        for (Frame frame : frames.frames()) {
            frame.identifier().getId()
                    .filter(id -> !id.isBlank())
                    .ifPresent(exposureIds::add);
        }
        return exposureIds;
    }

    static int resolveFrameDelayCentiseconds(double effectiveTicksPerFrame) {
        return Math.max(1, (int) Math.round(effectiveTicksPerFrame * 5.0d));
    }

    static String resolveFilename(DynamicSessionId sessionId, List<String> exposureIds) {
        if (!sessionId.value().isBlank()) {
            return sessionId.value() + "_dynamic";
        }

        if (!exposureIds.isEmpty()) {
            return exposureIds.getFirst() + "_dynamic";
        }

        return "dynamic_photograph";
    }

    private static ExportResult exportGif(DynamicSessionId sessionId, List<String> exposureIds,
                                          int frameDelayCentiseconds, boolean loop) {
        for (String exposureId : exposureIds) {
            ExposureClient.exposureStore().getOrRequest(exposureId);
        }

        ArrayList<Image> images = new ArrayList<>(exposureIds.size());
        OptionalLong creationDate = OptionalLong.empty();

        for (String exposureId : exposureIds) {
            ExposureData exposure = awaitExposure(exposureId);
            if (exposure == null) {
                continue;
            }

            if (creationDate.isEmpty()) {
                creationDate = OptionalLong.of(exposure.getTag().unixTimestamp());
            }

            images.add(PalettedImage.fromExposure(exposure));
        }

        if (images.isEmpty()) {
            return ExportResult.failed("No exposure frames were available for export.");
        }

        File[] exportedFile = new File[1];
        boolean exported = new AnimatedGifExporter(images, resolveFilename(sessionId, exposureIds))
                .setFrameDelayCentiseconds(frameDelayCentiseconds)
                .setLoop(loop)
                .toExposuresFolder()
                .organizeByWorld(Config.Client.EXPORT_ORGANIZE_BY_WORLD.get())
                .setCreationDate(creationDate.orElse(0L))
                .onExport(file -> exportedFile[0] = file)
                .export();

        if (!exported) {
            return ExportResult.failed("Animated gif export returned false.");
        }

        return ExportResult.success(images.size(), exposureIds.size(), exportedFile[0]);
    }

    private static ExposureData awaitExposure(String exposureId) {
        for (int attempt = 0; attempt < REQUEST_ATTEMPTS; attempt++) {
            var request = ExposureClient.exposureStore().getOrRequest(exposureId);
            if (request.getData().isPresent()) {
                return request.getData().get();
            }

            if (request.isError()) {
                return null;
            }

            try {
                Thread.sleep(REQUEST_DELAY_MILLIS);
            }
            catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        return null;
    }

    private static void displayMessage(MutableComponent message, boolean overlay) {
        Minecrft.execute(() -> {
            if (Minecrft.player() != null) {
                Minecrft.player().displayClientMessage(message, overlay);
            }
        });
    }

    private record ExportResult(boolean exported, int exportedFrameCount, int totalFrameCount, File outputFile,
                                String errorMessage) {
        private static ExportResult success(int exportedFrameCount, int totalFrameCount, File outputFile) {
            return new ExportResult(true, exportedFrameCount, totalFrameCount, outputFile, "");
        }

        private static ExportResult failed(String errorMessage) {
            return new ExportResult(false, 0, 0, null, errorMessage);
        }
    }
}
