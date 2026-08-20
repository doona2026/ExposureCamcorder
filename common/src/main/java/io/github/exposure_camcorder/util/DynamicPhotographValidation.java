package io.github.exposure_camcorder.util;

import io.github.exposure_camcorder.world.component.DynamicPhotographFrames;

public final class DynamicPhotographValidation {
    private DynamicPhotographValidation() {
    }

    public static int validateCaptureInterval(int captureIntervalTicks) {
        if (captureIntervalTicks <= 0) {
            throw new IllegalArgumentException("captureIntervalTicks must be positive.");
        }
        return captureIntervalTicks;
    }

    public static boolean canCreatePhotograph(int frameCount) {
        return frameCount >= 1;
    }

    public static void validateFrameCountForProduct(int frameCount) {
        if (!canCreatePhotograph(frameCount)) {
            throw new IllegalArgumentException("At least one frame is required to create a dynamic photograph.");
        }
    }

    public static int validateMaxFrames(int maxFrames) {
        if (maxFrames <= 0) {
            throw new IllegalArgumentException("maxFrames must be positive.");
        }
        if (maxFrames > DynamicPhotographFrames.MAX_FRAMES) {
            throw new IllegalArgumentException("maxFrames must be less than or equal to "
                    + DynamicPhotographFrames.MAX_FRAMES + '.');
        }
        return maxFrames;
    }
}
