package io.github.exposure_camcorder.client.render;

import java.util.LinkedHashSet;
import java.util.Set;

public class DynamicPhotographPrefetchPlanner {
    private final int radius;
    private final int maxFramesPerPlan;

    public DynamicPhotographPrefetchPlanner() {
        this(2, 8);
    }

    public DynamicPhotographPrefetchPlanner(int radius, int maxFramesPerPlan) {
        if (radius < 0) {
            throw new IllegalArgumentException("radius cannot be negative.");
        }
        if (maxFramesPerPlan <= 0) {
            throw new IllegalArgumentException("maxFramesPerPlan must be positive.");
        }

        this.radius = radius;
        this.maxFramesPerPlan = maxFramesPerPlan;
    }

    public Set<Integer> plan(int frameCount, int currentFrameIndex, int coverFrameIndex) {
        LinkedHashSet<Integer> indices = new LinkedHashSet<>();
        if (frameCount <= 0) {
            return indices;
        }

        addIfValid(indices, currentFrameIndex, frameCount);
        addIfValid(indices, coverFrameIndex, frameCount);

        for (int offset = 1; offset <= radius && indices.size() < maxFramesPerPlan; offset++) {
            addIfValid(indices, currentFrameIndex + offset, frameCount);
            if (indices.size() >= maxFramesPerPlan) {
                break;
            }
            addIfValid(indices, currentFrameIndex - offset, frameCount);
        }

        return indices;
    }

    private void addIfValid(Set<Integer> indices, int index, int frameCount) {
        if (indices.size() >= maxFramesPerPlan) {
            return;
        }
        if (index >= 0 && index < frameCount) {
            indices.add(index);
        }
    }
}
