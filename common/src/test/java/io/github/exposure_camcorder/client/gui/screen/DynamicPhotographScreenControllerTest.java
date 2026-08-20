package io.github.exposure_camcorder.client.gui.screen;

import io.github.exposure_camcorder.world.component.DynamicPhotographFrames;
import io.github.exposure_camcorder.world.component.DynamicPhotographSettings;
import io.github.exposure_camcorder.world.component.DynamicPhotographSummary;
import io.github.mortuusars.exposure.world.camera.frame.Frame;
import io.github.mortuusars.exposure.world.level.storage.ExposureIdentifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DynamicPhotographScreenControllerTest {
    @Test
    void openStartsFromFirstFrameAndAutoplaysByDefault() {
        DynamicPhotographScreenController controller = new DynamicPhotographScreenController();
        Frame first = frame("first");
        Frame second = frame("second");

        controller.open(new DynamicPhotographFrames(List.of(first, second)),
                new DynamicPhotographSettings(2, 2, true, 0),
                new DynamicPhotographSummary(2, 4, 0));

        DynamicPhotographViewModel viewModel = controller.viewModel();
        assertEquals(0, viewModel.currentFrameIndex());
        assertEquals(first, viewModel.currentFrame().orElseThrow());
        assertFalse(viewModel.paused());
        assertEquals("x1", viewModel.speedLabel());
    }

    @Test
    void tickAdvancesCurrentFrameAndPauseStopsFurtherPlayback() {
        DynamicPhotographScreenController controller = new DynamicPhotographScreenController();
        Frame first = frame("first");
        Frame second = frame("second");

        controller.open(new DynamicPhotographFrames(List.of(first, second)),
                new DynamicPhotographSettings(2, 2, true, 0),
                new DynamicPhotographSummary(2, 4, 0));

        controller.tick();
        assertEquals(0, controller.viewModel().currentFrameIndex());

        controller.tick();
        assertEquals(1, controller.viewModel().currentFrameIndex());
        assertEquals(second, controller.viewModel().currentFrame().orElseThrow());

        controller.togglePaused();
        controller.tick();
        assertEquals(1, controller.viewModel().currentFrameIndex());
        assertTrue(controller.viewModel().paused());
    }

    @Test
    void tickWithoutPlaybackAdvanceReusesViewModelInstance() {
        DynamicPhotographScreenController controller = new DynamicPhotographScreenController();

        controller.open(new DynamicPhotographFrames(List.of(frame("first"), frame("second"))),
                new DynamicPhotographSettings(2, 2, true, 0),
                new DynamicPhotographSummary(2, 4, 0));

        DynamicPhotographViewModel beforeTick = controller.viewModel();
        DynamicPhotographViewModel afterTick = controller.tick();

        assertSame(beforeTick, afterTick);
    }

    @Test
    void coverFallbackIsReportedWhenPreferredFrameIsMissing() {
        DynamicPhotographScreenController controller = new DynamicPhotographScreenController();
        Frame fallback = frame("fallback");

        controller.open(new DynamicPhotographFrames(List.of(Frame.EMPTY, fallback)),
                new DynamicPhotographSettings(2, 2, true, 0),
                new DynamicPhotographSummary(2, 4, 0));

        DynamicPhotographViewModel viewModel = controller.viewModel();
        assertEquals(fallback, viewModel.coverFrame().orElseThrow());
        assertTrue(viewModel.coverFrameFallback());
    }

    @Test
    void speedControlsUpdateSessionLevelPlaybackRate() {
        DynamicPhotographScreenController controller = new DynamicPhotographScreenController();

        controller.open(new DynamicPhotographFrames(List.of(frame("first"), frame("second"), frame("third"))),
                new DynamicPhotographSettings(2, 2, true, 0),
                new DynamicPhotographSummary(3, 6, 0));

        controller.speedUp();
        assertEquals(1.25d, controller.viewModel().speedMultiplier());
        assertEquals("x1.25", controller.viewModel().speedLabel());

        controller.resetSpeed();
        assertEquals(1.0d, controller.viewModel().speedMultiplier());
        assertEquals("x1", controller.viewModel().speedLabel());
    }

    private static Frame frame(String id) {
        return Frame.EMPTY.toMutable()
                .setIdentifier(ExposureIdentifier.id(id))
                .toImmutable();
    }
}
