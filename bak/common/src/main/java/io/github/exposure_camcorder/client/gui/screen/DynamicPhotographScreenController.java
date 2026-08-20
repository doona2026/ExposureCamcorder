package io.github.exposure_camcorder.client.gui.screen;

import io.github.exposure_camcorder.ExposureCamcorder;
import io.github.exposure_camcorder.client.gui.component.DynamicPlaybackControls;
import io.github.exposure_camcorder.client.playback.DynamicPlaybackController;
import io.github.exposure_camcorder.client.playback.DynamicPlaybackSession;
import io.github.exposure_camcorder.client.render.DynamicPhotographCoverResolver;
import io.github.exposure_camcorder.client.render.DynamicPhotographFrameResolver;
import io.github.exposure_camcorder.world.component.DynamicPhotographFrames;
import io.github.exposure_camcorder.world.component.DynamicPhotographSettings;
import io.github.exposure_camcorder.world.component.DynamicPhotographSummary;
import io.github.mortuusars.exposure.world.camera.frame.Frame;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class DynamicPhotographScreenController {
    private final DynamicPlaybackController playbackController;
    private final DynamicPhotographFrameResolver frameResolver;
    private final DynamicPhotographCoverResolver coverResolver;

    private DynamicPhotographFrames frames = DynamicPhotographFrames.EMPTY;
    private DynamicPhotographSettings settings = DynamicPhotographSettings.DEFAULT;
    private DynamicPhotographSummary summary = new DynamicPhotographSummary(0, 0, 0);
    private DynamicPlaybackControls controls = new DynamicPlaybackControls(DynamicPhotographSettings.DEFAULT.defaultPlaybackTicksPerFrame());
    private DynamicPlaybackSession playbackSession = new DynamicPlaybackSession(0,
            DynamicPhotographSettings.DEFAULT.defaultPlaybackTicksPerFrame(), false);
    private DynamicPhotographViewModel viewModel = DynamicPhotographViewModel.EMPTY;
    private Optional<Frame> cachedCurrentFrame = Optional.empty();
    private boolean cachedCurrentFrameFallback;
    private int cachedCurrentFrameIndex = -1;
    private Optional<Frame> cachedCoverFrame = Optional.empty();
    private boolean cachedCoverFrameFallback;

    public DynamicPhotographScreenController() {
        this(new DynamicPlaybackController(), new DynamicPhotographFrameResolver(), new DynamicPhotographCoverResolver());
    }

    public DynamicPhotographScreenController(DynamicPlaybackController playbackController,
                                             DynamicPhotographFrameResolver frameResolver,
                                             DynamicPhotographCoverResolver coverResolver) {
        this.playbackController = playbackController;
        this.frameResolver = frameResolver;
        this.coverResolver = coverResolver;
    }

    public void open(ItemStack stack) {
        DynamicPhotographFrames stackFrames = stack.getOrDefault(ExposureCamcorder.DataComponents.DYNAMIC_PHOTOGRAPH_FRAMES,
                DynamicPhotographFrames.EMPTY);
        DynamicPhotographSettings stackSettings = stack.getOrDefault(ExposureCamcorder.DataComponents.DYNAMIC_PHOTOGRAPH_SETTINGS,
                DynamicPhotographSettings.DEFAULT);
        DynamicPhotographSummary stackSummary = stack.getOrDefault(ExposureCamcorder.DataComponents.DYNAMIC_PHOTOGRAPH_SUMMARY,
                new DynamicPhotographSummary(stackFrames.size(), 0, 0));

        open(stackFrames, stackSettings, stackSummary);
    }

    public void open(DynamicPhotographFrames frames,
                     DynamicPhotographSettings settings,
                     DynamicPhotographSummary summary) {
        this.frames = frames;
        this.settings = settings;
        this.summary = summary;
        this.controls = new DynamicPlaybackControls(settings.defaultPlaybackTicksPerFrame());
        this.playbackSession = playbackController.createSession(summary, settings);
        this.cachedCurrentFrame = Optional.empty();
        this.cachedCurrentFrameFallback = false;
        this.cachedCurrentFrameIndex = -1;
        this.cachedCoverFrame = coverResolver.resolve(frames, summary.coverFrameIndex());
        this.cachedCoverFrameFallback = isFallback(summary.coverFrameIndex(), cachedCoverFrame);
        this.viewModel = rebuildViewModel();
    }

    public DynamicPhotographViewModel viewModel() {
        return viewModel;
    }

    public DynamicPlaybackSession playbackSession() {
        return playbackSession;
    }

    public DynamicPlaybackControls playbackControls() {
        return controls;
    }

    public DynamicPhotographViewModel tick() {
        if (!playbackController.tick(playbackSession)) {
            return viewModel;
        }
        return rebuildViewModel();
    }

    public DynamicPhotographViewModel togglePaused() {
        playbackSession.togglePaused();
        return rebuildViewModel();
    }

    public DynamicPhotographViewModel speedUp() {
        controls.speedUp(playbackSession);
        return rebuildViewModel();
    }

    public DynamicPhotographViewModel slowDown() {
        controls.slowDown(playbackSession);
        return rebuildViewModel();
    }

    public DynamicPhotographViewModel resetSpeed() {
        controls.resetSpeed(playbackSession);
        return rebuildViewModel();
    }

    public DynamicPhotographViewModel nextFrame() {
        playbackController.advanceToNextFrame(playbackSession);
        return rebuildViewModel();
    }

    public DynamicPhotographViewModel previousFrame() {
        playbackController.advanceToPreviousFrame(playbackSession);
        return rebuildViewModel();
    }

    public DynamicPhotographViewModel jumpToFrame(int frameIndex) {
        playbackController.jumpToFrame(playbackSession, frameIndex);
        return rebuildViewModel();
    }

    private DynamicPhotographViewModel rebuildViewModel() {
        int currentFrameIndex = playbackSession.currentFrameIndex();
        refreshCachedCurrentFrame(currentFrameIndex);

        viewModel = new DynamicPhotographViewModel(
                summary.frameCount(),
                currentFrameIndex,
                summary.coverFrameIndex(),
                playbackSession.paused(),
                playbackSession.canAnimate(),
                playbackSession.loop(),
                playbackSession.ticksPerFrame(),
                controls.speedMultiplier(playbackSession),
                controls.speedLabel(playbackSession),
                cachedCurrentFrame,
                cachedCurrentFrameFallback,
                cachedCoverFrame,
                cachedCoverFrameFallback
        );
        return viewModel;
    }

    private void refreshCachedCurrentFrame(int currentFrameIndex) {
        if (cachedCurrentFrameIndex == currentFrameIndex) {
            return;
        }

        cachedCurrentFrame = frameResolver.resolve(frames, currentFrameIndex);
        cachedCurrentFrameFallback = isFallback(currentFrameIndex, cachedCurrentFrame);
        cachedCurrentFrameIndex = currentFrameIndex;
    }

    private boolean isFallback(int preferredIndex, Optional<Frame> resolvedFrame) {
        if (resolvedFrame.isEmpty()) {
            return false;
        }

        Optional<Frame> preferredFrame = frames.getFrame(preferredIndex)
                .filter(frame -> !frame.identifier().isEmpty());

        return preferredFrame.isEmpty() || !preferredFrame.get().equals(resolvedFrame.get());
    }
}
