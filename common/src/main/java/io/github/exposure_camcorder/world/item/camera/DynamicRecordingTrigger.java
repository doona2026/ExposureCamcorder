package io.github.exposure_camcorder.world.item.camera;

import io.github.exposure_camcorder.Config;
import io.github.exposure_camcorder.ExposureCamcorder;
import io.github.exposure_camcorder.network.packet.s2c.DynamicCaptureStartS2CP;
import io.github.exposure_camcorder.network.packet.s2c.DynamicCaptureStateS2CP;
import io.github.exposure_camcorder.util.DynamicPhotographFactory;
import io.github.exposure_camcorder.util.DynamicPhotographFactory.DynamicPhotographCreationData;
import io.github.exposure_camcorder.world.session.DynamicCaptureSession;
import io.github.exposure_camcorder.world.session.DynamicCaptureSessionResult;
import io.github.exposure_camcorder.world.session.DynamicCaptureTicker;
import io.github.mortuusars.exposure.network.Packets;
import io.github.mortuusars.exposure.world.camera.capture.CaptureParameters;
import io.github.mortuusars.exposure.world.camera.film.properties.FilmProperties;
import io.github.mortuusars.exposure.world.entity.CameraHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import io.github.mortuusars.exposure.world.camera.frame.Frame;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DynamicRecordingTrigger {
    private final DynamicCameraModeController modeController = new DynamicCameraModeController();

    public boolean isRecording(Player player) {
        return ExposureCamcorder.captureSessionManager().getActiveSession(player.getUUID()).isPresent();
    }

    public boolean startUsing(Level level, Player player, ItemStack cameraStack) {
        DynamicCameraModeState state = modeController.syncState(cameraStack, player);
        if (!state.canStartRecording()) {
            return false;
        }

        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return true;
        }

        if (ExposureCamcorder.captureSessionManager().getActiveSession(player.getUUID()).isPresent()) {
            return true;
        }

        int maxFrames = modeController.getMaxFrames(cameraStack);
        int frameSize = modeController.getFrameSize(cameraStack);
        int durationBudgetTicks = calculateDurationBudgetTicks(maxFrames, state.captureIntervalTicks(), frameSize,
                io.github.mortuusars.exposure.Config.Server.DEFAULT_FRAME_SIZE.get(),
                Config.Server.MAX_RECORDING_DURATION_TICKS.get());
        String sessionId = UUID.randomUUID().toString();
        DynamicCaptureSession session = ExposureCamcorder.captureSessionManager().startSession(player.getUUID(), sessionId,
                level.getGameTime(), state.captureIntervalTicks(), maxFrames, durationBudgetTicks);

        CaptureParameters captureParameters = createCaptureParameters(serverPlayer, cameraStack, sessionId);
        ExposureCamcorder.LOGGER.info("Dynamic capture session '{}' started for '{}': maxFrames={}, interval={}ticks, budget={}ticks.",
                session.sessionId(), player.getScoreboardName(), session.maxFrames(),
                session.captureIntervalTicks(), session.maxRecordingDurationTicks());
        Packets.sendToClient(new DynamicCaptureStartS2CP(session.sessionId(), session.captureIntervalTicks(),
                session.maxFrames(), session.maxRecordingDurationTicks(), captureParameters), serverPlayer);
        return true;
    }

    int calculateDurationBudgetTicks(int maxFrames, int captureIntervalTicks, int frameSize, int defaultFrameSize,
                                     int minimumDurationTicks) {
        long defaultArea = (long) defaultFrameSize * defaultFrameSize;
        long frameAreaMultiplier = Math.max(1L,
                ((long) frameSize * frameSize + defaultArea - 1L) / defaultArea);
        long captureWindow = (long) maxFrames * (long) captureIntervalTicks * 4L * frameAreaMultiplier;
        return (int) Math.max(minimumDurationTicks, Math.min(Integer.MAX_VALUE, captureWindow));
    }

    public void tickServerPlayer(ServerPlayer player) {
        Optional<DynamicCaptureSession> sessionOpt = ExposureCamcorder.captureSessionManager()
                .getActiveSession(player.getUUID());
        if (sessionOpt.isEmpty()) {
            return;
        }

        DynamicCaptureSession session = sessionOpt.get();
        ItemStack cameraStack = resolveCameraStack(player);
        DynamicCaptureTicker.TickResult tickResult = ExposureCamcorder.captureTicker()
                .tickSession(session, player.level().getGameTime());
        if (tickResult.hasCompletedSession()) {
            finalizeCompletedSession(player, cameraStack, tickResult.completedSession());
        }
    }

    private CaptureParameters createCaptureParameters(ServerPlayer player, ItemStack cameraStack, String exposureId) {
        CaptureParameters.Builder builder = new CaptureParameters.Builder(exposureId)
                .setCropFactor(1f)
                .setFilmProperties(modeController.getFilmProperties(cameraStack));

        if (player instanceof CameraHolder holder) {
            builder.setCameraHolder(holder);
        }

        return builder.build();
    }

    private void finalizeCompletedSession(ServerPlayer player, ItemStack cameraStack, DynamicCaptureSessionResult result) {
        ExposureCamcorder.LOGGER.info("Dynamic capture session '{}' finished for '{}': reason={}, frames={}.",
                result.sessionId(), player.getScoreboardName(), result.endReason(), result.frameCount());
        Packets.sendToClient(new DynamicCaptureStateS2CP(result.sessionId(), result.frameCount(), true), player);
        ExposureCamcorder.captureSessionManager().removeSession(player.getUUID());

        List<Frame> combinedFrames = new ArrayList<>(modeController.getRecordedFrames(cameraStack));
        combinedFrames.addAll(result.frames().frames());

        if (result.shouldCreatePhotograph()) {
            DynamicPhotographCreationData creationData = DynamicPhotographFactory.buildCreationData(combinedFrames,
                    modeController.createPhotographSettings(cameraStack), result.sessionId());
            if (!modeController.storeCompletedRecording(cameraStack, creationData)) {
                ExposureCamcorder.LOGGER.error(
                        "Failed to persist dynamic recording on film for session '{}'; falling back to direct photograph delivery.",
                        result.sessionId());
                ItemStack photograph = DynamicPhotographFactory.create(combinedFrames,
                        modeController.createPhotographSettings(cameraStack), result.sessionId());
                boolean delivered = player.getInventory().add(photograph);
                if (!delivered) {
                    ItemEntity droppedPhotograph = player.drop(photograph, false);
                    delivered = droppedPhotograph != null;
                }

                if (!delivered) {
                    ExposureCamcorder.LOGGER.error("Failed to deliver fallback dynamic photograph for session '{}'.",
                            result.sessionId());
                }
            }
            player.displayClientMessage(Component.translatable(
                    "message.exposure_camcorder.recording.completed", result.frameCount()), true);
        } else if (result.frameCount() > 0) {
            DynamicPhotographCreationData creationData = DynamicPhotographFactory.buildCreationData(combinedFrames,
                    modeController.createPhotographSettings(cameraStack), result.sessionId());
            if (!modeController.storePartialRecording(cameraStack, creationData)) {
                ExposureCamcorder.LOGGER.error(
                        "Failed to persist partial dynamic recording for session '{}'.",
                        result.sessionId());
            }
            player.displayClientMessage(Component.translatable(
                    "message.exposure_camcorder.camera.recording.stopped"), true);
        } else {
            player.displayClientMessage(Component.translatable(
                    "message.exposure_camcorder.recording.failed_no_frames"), true);
        }
    }

    private ItemStack resolveCameraStack(ServerPlayer player) {
        ItemStack mainHandItem = player.getMainHandItem();
        if (modeController.findLoadedFilm(mainHandItem).isPresent()) {
            return mainHandItem;
        }

        ItemStack offhandItem = player.getOffhandItem();
        if (modeController.findLoadedFilm(offhandItem).isPresent()) {
            return offhandItem;
        }

        return mainHandItem;
    }
}
