package io.github.exposure_camcorder.client.render;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DynamicPhotographPrefetchPlannerTest {
    @Test
    void plansSlidingWindowAroundCurrentFrameAndCoverFrame() {
        DynamicPhotographPrefetchPlanner planner = new DynamicPhotographPrefetchPlanner(2, 8);

        assertEquals(List.of(10, 0, 11, 9, 12, 8), List.copyOf(planner.plan(40, 10, 0)));
    }

    @Test
    void skipsOutOfBoundsAndHonorsMaxFrameLimit() {
        DynamicPhotographPrefetchPlanner planner = new DynamicPhotographPrefetchPlanner(4, 4);

        assertEquals(List.of(1, 0, 2, 3), List.copyOf(planner.plan(5, 1, 0)));
    }
}
