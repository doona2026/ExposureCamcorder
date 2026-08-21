package io.github.exposure_camcorder.compatibility.exposure;

import io.github.exposure_camcorder.ExposureCamcorder;
import io.github.exposure_camcorder.client.capture.DynamicFrameCaptureClient;
import io.github.exposure_camcorder.world.item.camera.DynamicCameraModeController;
import io.github.exposure_camcorder.world.item.camera.DynamicCameraModeState;
import io.github.exposure_camcorder.world.item.camera.DynamicRecordingTrigger;
import io.github.exposure_camcorder.world.session.DynamicCaptureSessionEndReason;
import io.github.exposure_camcorder.world.item.DynamicFilmItem;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.world.entity.CameraOperator;
import io.github.mortuusars.exposure.world.item.camera.Attachment;
import io.github.mortuusars.exposure.world.item.camera.CameraItem;
import io.github.mortuusars.exposure.world.item.camera.ShutterState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

public final class ExposureCameraHooks {
    private static final DynamicCameraModeController MODE_CONTROLLER = new DynamicCameraModeController();
    private static final DynamicRecordingTrigger RECORDING_TRIGGER = new DynamicRecordingTrigger();

    private ExposureCameraHooks() {
    }

    public static boolean isDynamicCamera(ItemStack stack) {
        return stack.is(Exposure.Items.CAMERA.get());
    }

    public static Optional<ItemStack> findModeControlledCamera(Player player) {
        ItemStack mainHandItem = player.getMainHandItem();
        if (isDynamicCamera(mainHandItem) && isActiveCamera(mainHandItem)) {
            return Optional.of(mainHandItem);
        }

        ItemStack offhandItem = player.getOffhandItem();
        if (isDynamicCamera(offhandItem) && isActiveCamera(offhandItem)) {
            return Optional.of(offhandItem);
        }

        if (isDynamicCamera(mainHandItem)) {
            return Optional.of(mainHandItem);
        }

        if (isDynamicCamera(offhandItem)) {
            return Optional.of(offhandItem);
        }

        return Optional.empty();
    }

    public static DynamicCameraModeState toggleMode(ItemStack stack, Player player) {
        return MODE_CONTROLLER.toggleMode(stack, player);
    }

    public static void normalizeCameraRuntimeState(Level level, Player player, ItemStack stack) {
        if (level.isClientSide || !isDynamicCamera(stack) || !(stack.getItem() instanceof CameraItem)) {
            return;
        }
        CameraItem cameraItem = (CameraItem) stack.getItem();

        Attachment.FILM.ifPresent(stack, filmStack -> {
            if (!(filmStack.getItem() instanceof DynamicFilmItem)) {
                return;
            }
            DynamicFilmItem dynamicFilmItem = (DynamicFilmItem) filmStack.getItem();

            ItemStack sanitizedFilmStack = filmStack.copy();
            int actualFrameCount = dynamicFilmItem.getStoredFramesCount(sanitizedFilmStack);
            DynamicFilmItem.syncExposureCompatibilityComponents(sanitizedFilmStack, actualFrameCount,
                    dynamicFilmItem.getFrameSize(sanitizedFilmStack));

            if (!ItemStack.isSameItemSameTags(filmStack, sanitizedFilmStack)) {
                Attachment.FILM.set(stack, sanitizedFilmStack);
            }
        });

        boolean matchesActive = false;
        if (player instanceof CameraOperator) {
            CameraOperator cameraOperator = (CameraOperator) player;
            matchesActive = cameraOperator.getActiveExposureCameraOptional()
                    .map(camera -> camera.idMatches(cameraItem.getOrCreateId(stack)))
                    .orElse(false);
        }
        if (matchesActive) {
            MODE_CONTROLLER.syncState(stack, player);
            return;
        }

        if (cameraItem.isActive(stack)) {
            cameraItem.setActive(stack, false);
        }
        if (cameraItem.isDisassembled(stack)) {
            cameraItem.setDisassembled(stack, false);
        }
        if (cameraItem.getShutter().isOpen(stack)) {
            cameraItem.getShutter().setState(stack, ShutterState.closed());
        }
        if (cameraItem.getTimer().getStartTick(stack) >= 0L || cameraItem.getTimer().getEndTick(stack) >= 0L) {
            cameraItem.getTimer().stop(stack);
        }

        MODE_CONTROLLER.syncState(stack, player);
    }

    public static void displayModeMessage(Player player, DynamicCameraModeState state) {
        player.displayClientMessage(Component.translatable(state.isDynamic()
                ? "message.exposure_camcorder.camera.mode.dynamic"
                : "message.exposure_camcorder.camera.mode.regular"), true);
    }

    public static boolean handleDynamicCameraRelease(Level level, Player player, ItemStack stack) {
        if (!isDynamicCamera(stack)) {
            return false;
        }

        if (level.isClientSide) {
            if (DynamicFrameCaptureClient.hasActiveSession()) {
                if (DynamicFrameCaptureClient.consumeFreshPress()) {
                    DynamicFrameCaptureClient.requestStop(DynamicCaptureSessionEndReason.RELEASED);
                }
                return true;
            }
            DynamicFrameCaptureClient.clearFreshPress();
        } else {
            if (RECORDING_TRIGGER.isRecording(player)) {
                return true;
            }
        }

        DynamicCameraModeState state = MODE_CONTROLLER.syncState(stack, player);
        if (!state.isDynamic()) {
            return false;
        }

        if (!state.filmLoaded()) {
            boolean anyFilmAttached = Attachment.FILM.map(stack, ItemStack::copy).isPresent();
            player.displayClientMessage(Component.translatable(anyFilmAttached
                    ? "message.exposure_camcorder.camera.recording.film_unavailable"
                    : "message.exposure_camcorder.camera.recording.requires_film"), true);
            return true;
        }

        return RECORDING_TRIGGER.startUsing(level, player, stack);
    }

    private static boolean isActiveCamera(ItemStack stack) {
        return stack.getItem() instanceof CameraItem && ((CameraItem) stack.getItem()).isActive(stack);
    }
}
