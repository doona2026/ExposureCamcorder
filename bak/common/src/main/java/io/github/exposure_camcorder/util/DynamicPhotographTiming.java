package io.github.exposure_camcorder.util;

import java.util.Locale;

public final class DynamicPhotographTiming {
    private DynamicPhotographTiming() {
    }

    public static double ticksToSeconds(int ticks) {
        if (ticks < 0) {
            throw new IllegalArgumentException("ticks cannot be negative.");
        }
        return ticks / 20.0d;
    }

    public static int estimateDurationTicks(int frameCount, int captureIntervalTicks) {
        DynamicPhotographValidation.validateCaptureInterval(captureIntervalTicks);
        if (frameCount < 0) {
            throw new IllegalArgumentException("frameCount cannot be negative.");
        }
        return frameCount * captureIntervalTicks;
    }

    public static double estimateDurationSeconds(int frameCount, int captureIntervalTicks) {
        return ticksToSeconds(estimateDurationTicks(frameCount, captureIntervalTicks));
    }

    public static int calculateMaxFrames(int maxDurationTicks, int captureIntervalTicks) {
        if (maxDurationTicks <= 0) {
            throw new IllegalArgumentException("maxDurationTicks must be positive.");
        }
        DynamicPhotographValidation.validateCaptureInterval(captureIntervalTicks);
        return Math.max(1, maxDurationTicks / captureIntervalTicks);
    }

    public static String formatSeconds(double seconds) {
        return String.format(Locale.ROOT, "%.2f", seconds);
    }
}
