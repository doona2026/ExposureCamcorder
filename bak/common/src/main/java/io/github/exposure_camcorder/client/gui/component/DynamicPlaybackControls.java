package io.github.exposure_camcorder.client.gui.component;

import io.github.exposure_camcorder.client.gui.screen.DynamicPhotographScreenController;
import io.github.exposure_camcorder.client.playback.DynamicPlaybackSession;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

import java.util.Locale;

public class DynamicPlaybackControls {
    private static final double[] SPEED_LEVELS = {0.10d, 0.25d, 0.50d, 0.75d, 1.00d, 1.25d, 1.50d, 1.75d, 2.00d};
    private static final double SPEED_EPSILON = 0.0001d;

    private final int defaultTicksPerFrame;
    private Button pauseToggleButton;
    private Button speedDownButton;
    private Button speedUpButton;
    private Button resetSpeedButton;

    public DynamicPlaybackControls(int defaultTicksPerFrame) {
        if (defaultTicksPerFrame <= 0) {
            throw new IllegalArgumentException("defaultTicksPerFrame must be positive.");
        }

        this.defaultTicksPerFrame = defaultTicksPerFrame;
    }

    public int defaultTicksPerFrame() {
        return defaultTicksPerFrame;
    }

    public boolean canSpeedUp(DynamicPlaybackSession session) {
        return session.canAnimate() && speedLevelIndex(session) < SPEED_LEVELS.length - 1;
    }

    public boolean canSlowDown(DynamicPlaybackSession session) {
        return session.canAnimate() && speedLevelIndex(session) > 0;
    }

    public boolean speedUp(DynamicPlaybackSession session) {
        if (!canSpeedUp(session)) {
            return false;
        }

        session.setSpeedMultiplier(SPEED_LEVELS[speedLevelIndex(session) + 1]);
        return true;
    }

    public boolean slowDown(DynamicPlaybackSession session) {
        if (!canSlowDown(session)) {
            return false;
        }

        session.setSpeedMultiplier(SPEED_LEVELS[speedLevelIndex(session) - 1]);
        return true;
    }

    public void resetSpeed(DynamicPlaybackSession session) {
        session.setSpeedMultiplier(1.0d);
    }

    public double speedMultiplier(DynamicPlaybackSession session) {
        return session.speedMultiplier();
    }

    public String speedLabel(DynamicPlaybackSession session) {
        String label = String.format(Locale.ROOT, "%.2f", speedMultiplier(session))
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
        return "x" + label;
    }

    public List<AbstractWidget> createWidgets(DynamicPhotographScreenController controller, Runnable onStateChanged,
                                              int screenWidth, int screenHeight) {
        int centerX = screenWidth / 2;
        int y = screenHeight - 26;
        int gap = 4;
        int pauseButtonWidth = 52;
        int iconButtonWidth = 20;
        int speedLabelWidth = 52;
        int buttonHeight = 20;
        int totalWidth = pauseButtonWidth + iconButtonWidth + speedLabelWidth + iconButtonWidth + gap * 4;
        int startX = centerX - totalWidth / 2;

        pauseToggleButton = Button.builder(playPauseLabel(controller), button -> {
            controller.togglePaused();
            syncButtons(controller);
            onStateChanged.run();
        }).bounds(startX, y, pauseButtonWidth, buttonHeight).build();

        speedDownButton = Button.builder(Component.literal("-"), button -> {
            controller.slowDown();
            syncButtons(controller);
            onStateChanged.run();
        }).bounds(startX + pauseButtonWidth + gap, y, iconButtonWidth, buttonHeight).build();

        resetSpeedButton = Button.builder(Component.literal("1x"), button -> {
            controller.resetSpeed();
            syncButtons(controller);
            onStateChanged.run();
        }).bounds(startX + pauseButtonWidth + gap * 2 + iconButtonWidth, y, speedLabelWidth, buttonHeight).build();

        speedUpButton = Button.builder(Component.literal("+"), button -> {
            controller.speedUp();
            syncButtons(controller);
            onStateChanged.run();
        }).bounds(startX + pauseButtonWidth + gap * 3 + iconButtonWidth + speedLabelWidth, y,
                iconButtonWidth, buttonHeight).build();

        syncButtons(controller);

        return List.of(pauseToggleButton, speedDownButton, speedUpButton, resetSpeedButton);
    }

    public void syncButtons(DynamicPhotographScreenController controller) {
        if (pauseToggleButton == null) {
            return;
        }

        pauseToggleButton.setMessage(playPauseLabel(controller));
        speedDownButton.active = canSlowDown(controller.playbackSession());
        speedUpButton.active = canSpeedUp(controller.playbackSession());
        resetSpeedButton.setMessage(Component.literal(controller.viewModel().speedLabel()));
    }

    private Component playPauseLabel(DynamicPhotographScreenController controller) {
        return Component.literal(controller.viewModel().paused() ? "Play" : "Pause");
    }

    private int speedLevelIndex(DynamicPlaybackSession session) {
        double speedMultiplier = session.speedMultiplier();
        for (int index = 0; index < SPEED_LEVELS.length; index++) {
            if (Math.abs(SPEED_LEVELS[index] - speedMultiplier) < SPEED_EPSILON) {
                return index;
            }
        }

        return 4;
    }
}
