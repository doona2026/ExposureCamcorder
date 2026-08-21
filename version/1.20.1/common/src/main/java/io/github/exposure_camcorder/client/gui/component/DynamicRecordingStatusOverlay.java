package io.github.exposure_camcorder.client.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.exposure_camcorder.ExposureCamcorder;
import io.github.exposure_camcorder.client.capture.DynamicRecordingClientState;
import io.github.mortuusars.exposure.client.util.GuiUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.List;

public class DynamicRecordingStatusOverlay {
    public static final ResourceLocation RECORDING_INDICATOR_TEXTURE =
            ExposureCamcorder.resource("textures/gui/viewfinder/recording_indicator.png");

    private static final int INDICATOR_TEXTURE_SIZE = 16;
    private static final int INDICATOR_RENDER_SIZE = 10;
    private static final int PANEL_PADDING = 4;
    private static final int LINE_SPACING = 2;
    private static final int LINE_GAP = 4;
    private static final int PROGRESS_BAR_WIDTH = 96;
    private static final int PROGRESS_BAR_HEIGHT = 6;
    private static final int PROGRESS_BAR_GAP = 5;

    private static final float HUD_SCALE = 0.95f;

    private static final int TITLE_COLOR = 0xFFFF5B5B;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int SHADOW_COLOR = 0xFF000000;
    private static final int PROGRESS_BAR_BACKGROUND = 0xFF2A2A2A;
    private static final int PROGRESS_BAR_FILL = 0xFFFF5B5B;
    private static final int PROGRESS_BAR_BORDER = 0x66FFFFFF;

    public boolean shouldRender(DynamicRecordingClientState state) {
        return state != null && state.isActive();
    }

    public int recordedFrames(DynamicRecordingClientState state) {
        return state == null ? 0 : state.recordedFrames();
    }

    public int remainingCapacity(DynamicRecordingClientState state) {
        return state == null ? 0 : Math.max(0, state.remainingFrames());
    }

    public float progressFraction(DynamicRecordingClientState state) {
        if (state == null || state.maxFrames() <= 0) {
            return 0f;
        }

        return Mth.clamp((float) state.recordedFrames() / (float) state.maxFrames(), 0f, 1f);
    }

    public void render(GuiGraphics guiGraphics, Font font, int x, int y, DynamicRecordingClientState state) {
        if (!shouldRender(state)) {
            return;
        }

        List<Component> lines = List.of(
                Component.translatable("gui.exposure_camcorder.recording_status.rec"),
                Component.translatable("gui.exposure_camcorder.recording_status.frames", recordedFrames(state)),
                Component.translatable("gui.exposure_camcorder.recording_status.remaining_capacity", remainingCapacity(state))
        );
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(HUD_SCALE, HUD_SCALE, 1f);
        int lineY = renderStatusLines(guiGraphics, font, lines);
        int progressX = PANEL_PADDING + INDICATOR_RENDER_SIZE + LINE_GAP;
        int progressY = lineY + font.lineHeight + PROGRESS_BAR_GAP;
        renderProgressBar(guiGraphics, progressX, progressY, progressFraction(state));
        guiGraphics.pose().popPose();
    }

    public int getViewfinderOffsetX() {
        return 12;
    }

    public int getViewfinderOffsetY() {
        return 12;
    }

    public int getPanelWidth(Font font, DynamicRecordingClientState state) {
        List<Component> lines = List.of(
                Component.translatable("gui.exposure_camcorder.recording_status.rec"),
                Component.translatable("gui.exposure_camcorder.recording_status.frames", recordedFrames(state)),
                Component.translatable("gui.exposure_camcorder.recording_status.remaining_capacity", remainingCapacity(state))
        );
        int textWidth = Math.max(PROGRESS_BAR_WIDTH, lines.stream().mapToInt(font::width).max().orElse(0));
        int panelWidth = PANEL_PADDING * 2 + INDICATOR_RENDER_SIZE + LINE_GAP + textWidth;
        return Math.round(panelWidth * HUD_SCALE);
    }

    private int renderStatusLines(GuiGraphics guiGraphics, Font font, List<Component> lines) {
        int iconX = PANEL_PADDING;
        int iconY = PANEL_PADDING + Math.max(0, (font.lineHeight - INDICATOR_RENDER_SIZE) / 2);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        guiGraphics.blit(RECORDING_INDICATOR_TEXTURE, iconX, iconY, 0, 0,
                INDICATOR_RENDER_SIZE, INDICATOR_RENDER_SIZE, INDICATOR_TEXTURE_SIZE, INDICATOR_TEXTURE_SIZE);

        int textX = iconX + INDICATOR_RENDER_SIZE + LINE_GAP;
        int lineY = PANEL_PADDING;
        drawLine(guiGraphics, font, lines.get(0), textX, lineY, TITLE_COLOR);
        for (int index = 1; index < lines.size(); index++) {
            lineY += font.lineHeight + LINE_SPACING;
            drawLine(guiGraphics, font, lines.get(index), textX, lineY, TEXT_COLOR);
        }
        return lineY;
    }

    private void renderProgressBar(GuiGraphics guiGraphics, int x, int y, float progress) {
        GuiUtil.drawRect(guiGraphics, x, y, PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT, PROGRESS_BAR_BACKGROUND);
        GuiUtil.drawRect(guiGraphics, x, y, Math.max(0, Math.round(PROGRESS_BAR_WIDTH * progress)),
                PROGRESS_BAR_HEIGHT, PROGRESS_BAR_FILL);
        guiGraphics.renderOutline(x, y, PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT, PROGRESS_BAR_BORDER);
    }

    private void drawLine(GuiGraphics guiGraphics, Font font, Component component, int x, int y, int color) {
        guiGraphics.drawString(font, component, x + 1, y + 1, SHADOW_COLOR, false);
        guiGraphics.drawString(font, component, x, y, color, false);
    }
}
