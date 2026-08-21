package io.github.exposure_camcorder.client.playback;

public class DynamicPlaybackSession {
    private final int frameCount;
    private int currentFrameIndex;
    private final int defaultTicksPerFrame;
    private final boolean loop;
    private boolean paused;
    private double tickProgress;
    private double speedMultiplier;

    public DynamicPlaybackSession(int frameCount, int ticksPerFrame, boolean loop) {
        if (frameCount < 0) {
            throw new IllegalArgumentException("frameCount cannot be negative.");
        }
        if (ticksPerFrame <= 0) {
            throw new IllegalArgumentException("ticksPerFrame must be positive.");
        }

        this.frameCount = frameCount;
        this.defaultTicksPerFrame = ticksPerFrame;
        this.loop = loop;
        this.paused = frameCount <= 1;
        this.tickProgress = 0.0d;
        this.speedMultiplier = 1.0d;
        this.currentFrameIndex = 0;
    }

    public int frameCount() {
        return frameCount;
    }

    public int currentFrameIndex() {
        return currentFrameIndex;
    }

    public int ticksPerFrame() {
        return Math.max(1, (int) Math.round(effectiveTicksPerFrame()));
    }

    public int defaultTicksPerFrame() {
        return defaultTicksPerFrame;
    }

    public boolean loop() {
        return loop;
    }

    public boolean paused() {
        return paused;
    }

    public double tickProgress() {
        return tickProgress;
    }

    public double speedMultiplier() {
        return speedMultiplier;
    }

    public double effectiveTicksPerFrame() {
        return defaultTicksPerFrame / speedMultiplier;
    }

    public boolean hasFrames() {
        return frameCount > 0;
    }

    public boolean canAnimate() {
        return frameCount > 1;
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        if (canAnimate()) {
            paused = false;
        }
    }

    public void togglePaused() {
        if (paused) {
            resume();
        } else {
            pause();
        }
    }

    public void setSpeedMultiplier(double speedMultiplier) {
        if (speedMultiplier <= 0.0d) {
            throw new IllegalArgumentException("speedMultiplier must be positive.");
        }
        this.speedMultiplier = speedMultiplier;
        this.tickProgress = 0.0d;
    }

    public void setCurrentFrameIndex(int frameIndex) {
        if (!hasFrames()) {
            currentFrameIndex = 0;
            tickProgress = 0.0d;
            return;
        }

        if (frameIndex < 0 || frameIndex >= frameCount) {
            throw new IllegalArgumentException("frameIndex is out of bounds: " + frameIndex);
        }

        currentFrameIndex = frameIndex;
        tickProgress = 0.0d;
    }

    public void resetProgress() {
        tickProgress = 0.0d;
    }

    public void advanceProgress() {
        tickProgress += speedMultiplier;
    }

    public boolean shouldAdvanceFrame() {
        return tickProgress >= defaultTicksPerFrame;
    }

    public void consumeFrameAdvance() {
        tickProgress -= defaultTicksPerFrame;
        if (tickProgress < 0.0d) {
            tickProgress = 0.0d;
        }
    }
}
