package io.github.exposure_camcorder.world.item.camera;

import io.github.exposure_camcorder.Config;
import io.github.exposure_camcorder.ExposureCamcorder;
import io.github.exposure_camcorder.util.DynamicPhotographFactory.DynamicPhotographCreationData;
import io.github.exposure_camcorder.world.component.DynamicPhotographSettings;
import io.github.exposure_camcorder.world.item.DynamicFilmItem;
import io.github.exposure_camcorder.world.item.DynamicMode;
import io.github.mortuusars.exposure.world.camera.film.properties.FilmProperties;
import io.github.mortuusars.exposure.world.item.camera.Attachment;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class DynamicCameraModeController {
    public DynamicCameraModeState getState(ItemStack stack) {
        return stack.getOrDefault(ExposureCamcorder.DataComponents.DYNAMIC_CAMERA_MODE_STATE, defaultState());
    }

    public DynamicCameraModeState syncState(ItemStack stack, Player player) {
        DynamicCameraModeState synced = getState(stack).withFilmLoaded(findLoadedFilm(stack).isPresent());
        stack.set(ExposureCamcorder.DataComponents.DYNAMIC_CAMERA_MODE_STATE, synced);
        return synced;
    }

    public DynamicCameraModeState toggleMode(ItemStack stack, Player player) {
        DynamicCameraModeState updated = syncState(stack, player).withMode(getState(stack).mode().next());
        stack.set(ExposureCamcorder.DataComponents.DYNAMIC_CAMERA_MODE_STATE, updated);
        return updated;
    }

    public boolean canStartRecording(ItemStack stack, Player player) {
        return syncState(stack, player).canStartRecording();
    }

    public Optional<ItemStack> findLoadedFilm(ItemStack cameraStack) {
        return Attachment.FILM.map(cameraStack, ItemStack::copy)
                .filter(stack -> stack.getItem() instanceof DynamicFilmItem dynamicFilmItem
                        && dynamicFilmItem.getRemainingFrames(stack) > 0
                        && !dynamicFilmItem.hasRecordedPhotograph(stack));
    }

    public int getMaxFrames(ItemStack cameraStack) {
        return findLoadedFilm(cameraStack)
                .map(stack -> ((DynamicFilmItem) stack.getItem()).getRemainingFrames(stack))
                .orElse(Config.Server.DEFAULT_DYNAMIC_FILM_MAX_FRAMES.get());
    }

    public FilmProperties getFilmProperties(ItemStack cameraStack) {
        return findLoadedFilm(cameraStack)
                .map(stack -> ((DynamicFilmItem) stack.getItem()).getFilmProperties(stack))
                .orElse(FilmProperties.EMPTY);
    }

    public int getFrameSize(ItemStack cameraStack) {
        return getFilmProperties(cameraStack).getSize();
    }

    public boolean consumeLoadedFilm(ItemStack cameraStack, int recordedFrames) {
        if (recordedFrames <= 0) {
            return false;
        }

        Optional<ItemStack> filmStack = Attachment.FILM.map(cameraStack, ItemStack::copy)
                .filter(stack -> stack.getItem() instanceof DynamicFilmItem);
        if (filmStack.isEmpty()) {
            return false;
        }

        ItemStack updatedFilmStack = filmStack.get();
        DynamicFilmItem filmItem = (DynamicFilmItem) updatedFilmStack.getItem();
        filmItem.addUsedFrames(updatedFilmStack, recordedFrames);

        if (filmItem.getRemainingFrames(updatedFilmStack) > 0) {
            Attachment.FILM.set(cameraStack, updatedFilmStack);
        } else {
            Attachment.FILM.set(cameraStack, ItemStack.EMPTY);
        }
        return true;
    }

    public boolean storeCompletedRecording(ItemStack cameraStack, DynamicPhotographCreationData creationData) {
        Optional<ItemStack> filmStack = Attachment.FILM.map(cameraStack, ItemStack::copy)
                .filter(stack -> stack.getItem() instanceof DynamicFilmItem);
        if (filmStack.isEmpty()) {
            return false;
        }

        ItemStack updatedFilmStack = filmStack.get();
        DynamicFilmItem filmItem = (DynamicFilmItem) updatedFilmStack.getItem();
        filmItem.setRecordedPhotograph(updatedFilmStack, creationData);
        Attachment.FILM.set(cameraStack, updatedFilmStack);
        return true;
    }

    public DynamicPhotographSettings createPhotographSettings(ItemStack stack) {
        DynamicCameraModeState state = getState(stack);
        return new DynamicPhotographSettings(state.captureIntervalTicks(),
                state.defaultPlaybackTicksPerFrame(), true, 0);
    }

    public DynamicCameraModeState defaultState() {
        return new DynamicCameraModeState(DynamicMode.REGULAR,
                Config.Server.DEFAULT_CAPTURE_INTERVAL_TICKS.get(),
                Config.Server.DEFAULT_CAPTURE_INTERVAL_TICKS.get(),
                false);
    }
}
