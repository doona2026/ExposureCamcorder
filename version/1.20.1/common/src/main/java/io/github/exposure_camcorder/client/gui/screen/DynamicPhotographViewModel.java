package io.github.exposure_camcorder.client.gui.screen;

import io.github.mortuusars.exposure.world.camera.frame.Frame;

import java.util.Optional;

public record DynamicPhotographViewModel(int frameCount,
                                         int currentFrameIndex,
                                         int coverFrameIndex,
                                         boolean paused,
                                         boolean canAnimate,
                                         boolean loop,
                                         int ticksPerFrame,
                                         double speedMultiplier,
                                         String speedLabel,
                                         Optional<Frame> currentFrame,
                                         boolean currentFrameFallback,
                                         Optional<Frame> coverFrame,
                                         boolean coverFrameFallback) {
    public static final DynamicPhotographViewModel EMPTY = new DynamicPhotographViewModel(0, 0, 0,
            true, false, false, 1, 1.0d, "x1.00", Optional.empty(), false, Optional.empty(), false);

    public DynamicPhotographViewModel {
        if (frameCount < 0) {
            throw new IllegalArgumentException("frameCount cannot be negative.");
        }
        if (currentFrameIndex < 0) {
            throw new IllegalArgumentException("currentFrameIndex cannot be negative.");
        }
        if (coverFrameIndex < 0) {
            throw new IllegalArgumentException("coverFrameIndex cannot be negative.");
        }
        if (ticksPerFrame <= 0) {
            throw new IllegalArgumentException("ticksPerFrame must be positive.");
        }
        if (speedMultiplier <= 0.0d) {
            throw new IllegalArgumentException("speedMultiplier must be positive.");
        }

        speedLabel = speedLabel == null ? "x1.00" : speedLabel;
        currentFrame = currentFrame == null ? Optional.empty() : currentFrame;
        coverFrame = coverFrame == null ? Optional.empty() : coverFrame;
    }

    public boolean hasCurrentFrame() {
        return currentFrame.isPresent();
    }

    public boolean hasCoverFrame() {
        return coverFrame.isPresent();
    }
}
