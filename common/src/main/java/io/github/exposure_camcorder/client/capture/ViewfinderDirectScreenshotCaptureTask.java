package io.github.exposure_camcorder.client.capture;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.mortuusars.exposure.client.image.Image;
import io.github.mortuusars.exposure.client.image.WrappedNativeImage;
import io.github.mortuusars.exposure.util.cycles.task.Result;
import io.github.mortuusars.exposure.util.cycles.task.Task;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.concurrent.CompletableFuture;

public class ViewfinderDirectScreenshotCaptureTask extends Task<Result<Image>> {
    private static final ArrayDeque<ViewfinderDirectScreenshotCaptureTask> PENDING_CAPTURES = new ArrayDeque<>();

    private @Nullable CompletableFuture<Result<Image>> future;
    private boolean queued;

    @Override
    public @NotNull CompletableFuture<Result<Image>> execute() {
        if (future == null) {
            future = new CompletableFuture<>();
        }

        if (!queued) {
            PENDING_CAPTURES.addLast(this);
            queued = true;
        }
        return future;
    }

    public static void capturePending() {
        ViewfinderDirectScreenshotCaptureTask capture = PENDING_CAPTURES.pollFirst();
        if (capture == null) {
            return;
        }

        capture.queued = false;
        capture.captureNow();
    }

    private void captureNow() {
        if (future == null || future.isDone()) {
            return;
        }

        try {
            NativeImage nativeImage = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget());
            future.complete(Result.success(new WrappedNativeImage(nativeImage)));
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
    }
}
