package io.github.exposure_camcorder.compatibility.exposure;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.exposure_camcorder.ExposureCamcorder;
import io.github.exposure_camcorder.client.export.DynamicPhotographGifExport;
import io.github.exposure_camcorder.client.render.DynamicPhotographPrefetchPlanner;
import io.github.exposure_camcorder.client.gui.screen.DynamicPhotographScreenController;
import io.github.exposure_camcorder.client.gui.screen.DynamicPhotographViewModel;
import io.github.exposure_camcorder.world.component.DynamicPhotographFrames;
import io.github.exposure_camcorder.world.component.DynamicPhotographSummary;
import io.github.exposure_camcorder.world.item.DynamicPhotographItem;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.client.image.renderable.RenderableImage;
import io.github.mortuusars.exposure.client.gui.screen.PhotographScreen;
import io.github.mortuusars.exposure.client.render.photograph.PhotographStyle;
import io.github.mortuusars.exposure.client.render.texture.TextureRenderer;
import io.github.mortuusars.exposure.client.util.Minecrft;
import io.github.mortuusars.exposure.world.camera.frame.Frame;
import io.github.mortuusars.exposure.world.item.PhotographItem;
import io.github.mortuusars.exposure.world.item.util.ItemAndStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;

public final class ExposurePhotographScreenHooks {
    private static final int PREFETCH_WINDOW_RADIUS = 2;
    private static final int MAX_REQUESTED_EXPOSURES = 48;
    private static final DynamicPhotographPrefetchPlanner PREFETCH_PLANNER =
            new DynamicPhotographPrefetchPlanner(PREFETCH_WINDOW_RADIUS, 8);
    private static final WeakHashMap<PhotographScreen, DynamicPhotographScreenState> DYNAMIC_SCREENS = new WeakHashMap<>();

    private ExposurePhotographScreenHooks() {
    }

    public static boolean openDynamicPhotographFromInventorySlot(int slot) {
        LocalPlayer player = Minecrft.player();
        if (player == null) {
            return false;
        }

        ItemStack sourceStack = player.getInventory().getItem(slot);
        if (!isDynamicPhotograph(sourceStack)) {
            return false;
        }

        return openDynamicPhotograph(sourceStack, slot);
    }

    public static boolean openDynamicPhotograph(ItemStack sourceStack) {
        return openDynamicPhotograph(sourceStack, -1);
    }

    private static boolean openDynamicPhotograph(ItemStack sourceStack, int slot) {
        if (!isDynamicPhotograph(sourceStack)) {
            return false;
        }

        DynamicPhotographScreenController controller = new DynamicPhotographScreenController();
        controller.open(sourceStack);

        Optional<Frame> initialFrame = resolveReadyDisplayFrame(controller.viewModel(), Optional.empty());
        ItemStack displayStack = createDisplayPhotographStack(initialFrame.orElse(Frame.EMPTY));
        PhotographScreen screen = new PhotographScreen(List.of(new ItemAndStack<>(displayStack)));

        DynamicPhotographScreenState state = new DynamicPhotographScreenState(slot, sourceStack.copy(), controller);
        state.lastReadyFrame = initialFrame;
        DYNAMIC_SCREENS.put(screen, state);
        requestDisplayFrameWindow(state, state.viewModel());
        Minecrft.get().setScreen(screen);
        return true;
    }

    public static void onPhotographScreenTick(PhotographScreen screen) {
        DynamicPhotographScreenState state = DYNAMIC_SCREENS.get(screen);
        if (state == null) {
            return;
        }

        state.refreshSourceStack();
        state.tick();
        requestDisplayFrameWindow(state, state.viewModel());
        syncDisplayedPhotograph(screen, state, state.viewModel());
        state.syncControls();
    }

    public static void onPhotographScreenInit(PhotographScreen screen) {
        DynamicPhotographScreenState state = DYNAMIC_SCREENS.get(screen);
        if (state == null) {
            return;
        }

        state.installControls(screen);
    }

    public static boolean onPhotographScreenKeyPressed(PhotographScreen screen, int keyCode, int scanCode, int modifiers) {
        DynamicPhotographScreenState state = DYNAMIC_SCREENS.get(screen);
        if (state == null) {
            return false;
        }

        if (keyCode == GLFW.GLFW_KEY_E && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            return state.exportGif();
        }

        boolean handled = switch (keyCode) {
            case InputConstants.KEY_SPACE -> {
                state.togglePaused();
                yield true;
            }
            case GLFW.GLFW_KEY_LEFT_BRACKET -> {
                state.slowDown();
                yield true;
            }
            case GLFW.GLFW_KEY_RIGHT_BRACKET -> {
                state.speedUp();
                yield true;
            }
            case GLFW.GLFW_KEY_BACKSLASH -> {
                state.resetSpeed();
                yield true;
            }
            default -> false;
        };

        if (!handled) {
            return false;
        }

        requestDisplayFrameWindow(state, state.viewModel());
        syncDisplayedPhotograph(screen, state, state.viewModel());
        playControlSound();
        return true;
    }

    public static boolean isDynamicPhotograph(ItemStack stack) {
        return stack.getItem() instanceof DynamicPhotographItem;
    }

    public static void appendDynamicPhotographTooltipLines(PhotographScreen screen, List<Component> lines) {
        if (!DYNAMIC_SCREENS.containsKey(screen)) {
            return;
        }

        lines.add(Component.translatable("gui.exposure_camcorder.dynamic_photograph.export_tooltip", "CTRL+E"));
    }

    public static boolean renderDynamicPhotographScreen(PhotographScreen screen, PoseStack poseStack,
                                                        MultiBufferSource bufferSource, int packedLight,
                                                        int r, int g, int b, int a) {
        DynamicPhotographScreenState state = DYNAMIC_SCREENS.get(screen);
        if (state == null) {
            return false;
        }

        DynamicPhotographViewModel viewModel = state.viewModel();
        Optional<Frame> displayFrame = resolveReadyDisplayFrame(viewModel, state.lastReadyFrame);
        displayFrame.ifPresent(frame -> state.lastReadyFrame = Optional.of(frame));

        PhotographStyle style = PhotographStyle.of(state.sourceStack);
        renderDynamicPhotographPaper(style, state.paperRotationDegrees, poseStack, bufferSource, packedLight, r, g, b, a);
        renderDynamicPhotographImage(style, displayFrame.orElse(Frame.EMPTY), poseStack, bufferSource, packedLight, r, g, b, a);
        renderDynamicPhotographOverlay(style, state.paperRotationDegrees, poseStack, bufferSource, packedLight, r, g, b, a);
        return true;
    }

    private static void syncDisplayedPhotograph(PhotographScreen screen, DynamicPhotographScreenState state,
                                                DynamicPhotographViewModel viewModel) {
        Optional<Frame> displayFrame = resolveReadyDisplayFrame(viewModel, state.lastReadyFrame);
        displayFrame.ifPresent(frame -> state.lastReadyFrame = Optional.of(frame));
        ItemStack displayStack = createDisplayPhotographStack(displayFrame.orElse(Frame.EMPTY));
        ItemAndStack<PhotographItem> displayPhotograph = new ItemAndStack<>(displayStack);
        PhotographScreenAccess accessor = (PhotographScreenAccess) screen;
        ArrayList<ItemAndStack<PhotographItem>> photographs = accessor.exposureCamcorder$getPhotographs();

        if (photographs.isEmpty()) {
            accessor.exposureCamcorder$setPhotographs(List.of(displayPhotograph));
        } else {
            photographs.set(0, displayPhotograph);
        }
    }

    private static Optional<Frame> resolveDisplayFrame(DynamicPhotographViewModel viewModel) {
        return viewModel.currentFrame().or(() -> viewModel.coverFrame());
    }

    private static Optional<Frame> resolveReadyDisplayFrame(DynamicPhotographViewModel viewModel, Optional<Frame> lastReadyFrame) {
        Optional<Frame> currentReadyFrame = viewModel.currentFrame().filter(ExposurePhotographScreenHooks::isFrameReady);
        if (currentReadyFrame.isPresent()) {
            return currentReadyFrame;
        }

        Optional<Frame> lastReadyLoaded = lastReadyFrame.filter(ExposurePhotographScreenHooks::isFrameReady);
        if (lastReadyLoaded.isPresent()) {
            return lastReadyLoaded;
        }

        return viewModel.coverFrame().filter(ExposurePhotographScreenHooks::isFrameReady);
    }

    private static DynamicPhotographFrames getDynamicPhotographFrames(ItemStack sourceStack) {
        DynamicPhotographFrames frames = sourceStack.getOrDefault(ExposureCamcorder.DataComponents.DYNAMIC_PHOTOGRAPH_FRAMES,
                DynamicPhotographFrames.EMPTY);
        return frames;
    }

    private static void requestDisplayFrameWindow(DynamicPhotographScreenState state, DynamicPhotographViewModel viewModel) {
        DynamicPhotographFrames frames = getDynamicPhotographFrames(state.sourceStack);
        for (int frameIndex : PREFETCH_PLANNER.plan(viewModel.frameCount(), viewModel.currentFrameIndex(),
                viewModel.coverFrameIndex())) {
            frames.getFrame(frameIndex)
                    .map(Frame::identifier)
                    .ifPresent(identifier -> identifier.ifId(state::requestExposure));
        }
    }

    private static boolean isFrameReady(Frame frame) {
        if (frame == Frame.EMPTY || frame.identifier().isEmpty()) {
            return false;
        }

        return frame.identifier().map(id -> ExposureClient.exposureStore().getOrRequest(id).getData().isPresent(),
                texture -> true);
    }

    private static ItemStack createDisplayPhotographStack(Frame frame) {
        ItemStack displayStack = new ItemStack(Exposure.Items.PHOTOGRAPH.get());
        if (frame != Frame.EMPTY) {
            displayStack.set(Exposure.DataComponents.PHOTOGRAPH_FRAME, frame);
            displayStack.set(Exposure.DataComponents.PHOTOGRAPH_TYPE, frame.type());
        }
        return displayStack;
    }

    public static void playControlSound() {
        LocalPlayer player = Minecrft.player();
        if (player != null) {
            player.playSound(Exposure.SoundEvents.CAMERA_LENS_RING_CLICK.get(), 0.8f, 1.0f);
        }
    }

    public static void playOpenSound() {
        if (Minecrft.level() == null) {
            return;
        }

        Minecrft.get().getSoundManager().play(SimpleSoundInstance.forUI(Exposure.SoundEvents.PHOTOGRAPH_RUSTLE.get(),
                Minecrft.level().getRandom().nextFloat() * 0.2f + 1.3f, 0.75f));
    }

    private static void renderDynamicPhotographPaper(PhotographStyle style, int paperRotationDegrees, PoseStack poseStack,
                                                     MultiBufferSource bufferSource, int packedLight,
                                                     int r, int g, int b, int a) {
        if (style.paperTexture() == ExposureClient.Textures.EMPTY) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5f, 0.5f, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(paperRotationDegrees));
        poseStack.translate(-0.5f, -0.5f, 0);
        TextureRenderer.render(poseStack, bufferSource, style.paperTexture(), packedLight, r, g, b, a);
        poseStack.popPose();
    }

    private static void renderDynamicPhotographImage(PhotographStyle style, Frame displayFrame, PoseStack poseStack,
                                                     MultiBufferSource bufferSource, int packedLight,
                                                     int r, int g, int b, int a) {
        if (displayFrame == Frame.EMPTY) {
            return;
        }

        RenderableImage image = style.process(ExposureClient.renderedExposures().getOrCreate(displayFrame));
        if (image.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        float offset = 0.0625f;
        poseStack.translate(offset, offset, 0.001);
        poseStack.scale(0.875f, 0.875f, 0.875f);
        ExposureClient.imageRenderer().render(image, poseStack, bufferSource,
                io.github.mortuusars.exposure.client.render.image.RenderCoordinates.DEFAULT,
                packedLight, r, g, b, a);
        poseStack.popPose();
    }

    private static void renderDynamicPhotographOverlay(PhotographStyle style, int paperRotationDegrees, PoseStack poseStack,
                                                       MultiBufferSource bufferSource, int packedLight,
                                                       int r, int g, int b, int a) {
        if (!style.hasOverlayTexture()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5f, 0.5f, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(paperRotationDegrees));
        poseStack.translate(-0.5f, -0.5f, 0);
        poseStack.translate(0, 0, 0.002);
        TextureRenderer.render(poseStack, bufferSource, style.overlayTexture(), packedLight, r, g, b, a);
        poseStack.popPose();
    }

    private static int resolvePaperRotationDegrees(DynamicPhotographFrames frames, DynamicPhotographSummary summary) {
        Optional<Frame> preferredFrame = frames.getFrame(summary.coverFrameIndex())
                .filter(frame -> frame != Frame.EMPTY && !frame.identifier().isEmpty());
        if (preferredFrame.isPresent()) {
            return preferredFrame.get().identifier().hashCode() % 4 * 90;
        }
        return 0;
    }

    private static final class DynamicPhotographScreenState {
        private final int slot;
        private final DynamicPhotographScreenController controller;
        private final Set<String> requestedExposureIds = new HashSet<>();
        private final ArrayDeque<String> requestOrder = new ArrayDeque<>();
        private ItemStack sourceStack;
        private Optional<Frame> lastReadyFrame = Optional.empty();
        private int paperRotationDegrees;

        private DynamicPhotographScreenState(int slot, ItemStack sourceStack, DynamicPhotographScreenController controller) {
            this.slot = slot;
            this.sourceStack = sourceStack;
            this.controller = controller;
            DynamicPhotographFrames frames = getDynamicPhotographFrames(sourceStack);
            DynamicPhotographSummary summary = sourceStack.getOrDefault(ExposureCamcorder.DataComponents.DYNAMIC_PHOTOGRAPH_SUMMARY,
                    new DynamicPhotographSummary(frames.size(), 0, 0));
            this.paperRotationDegrees = resolvePaperRotationDegrees(frames, summary);
        }

        public DynamicPhotographViewModel viewModel() {
            return controller.viewModel();
        }

        public void tick() {
            controller.tick();
        }

        public void togglePaused() {
            controller.togglePaused();
        }

        public void speedUp() {
            controller.speedUp();
        }

        public void slowDown() {
            controller.slowDown();
        }

        public void resetSpeed() {
            controller.resetSpeed();
        }

        public void installControls(PhotographScreen screen) {
            List<AbstractWidget> widgets = controller.playbackControls().createWidgets(controller, () -> {
                requestDisplayFrameWindow(this, controller.viewModel());
                syncDisplayedPhotograph(screen, this, controller.viewModel());
                playControlSound();
            }, screen.width, screen.height);
            for (AbstractWidget widget : widgets) {
                ((Screen) screen).addRenderableWidget(widget);
            }
        }

        public void syncControls() {
            controller.playbackControls().syncButtons(controller);
        }

        public void refreshSourceStack() {
            if (slot < 0) {
                return;
            }

            LocalPlayer player = Minecrft.player();
            if (player == null) {
                return;
            }

            ItemStack liveStack = player.getInventory().getItem(slot);
            if (!isDynamicPhotograph(liveStack) || ItemStack.matches(sourceStack, liveStack)) {
                return;
            }

            sourceStack = liveStack.copy();
            controller.open(liveStack);
            lastReadyFrame = resolveReadyDisplayFrame(controller.viewModel(), Optional.empty());
            DynamicPhotographFrames frames = getDynamicPhotographFrames(sourceStack);
            DynamicPhotographSummary summary = sourceStack.getOrDefault(ExposureCamcorder.DataComponents.DYNAMIC_PHOTOGRAPH_SUMMARY,
                    new DynamicPhotographSummary(frames.size(), 0, 0));
            paperRotationDegrees = resolvePaperRotationDegrees(frames, summary);
            requestedExposureIds.clear();
            requestOrder.clear();
        }

        public boolean exportGif() {
            return DynamicPhotographGifExport.export(sourceStack.copy(),
                    controller.playbackSession().effectiveTicksPerFrame(),
                    controller.playbackSession().loop());
        }

        public void requestExposure(String exposureId) {
            if (!requestedExposureIds.add(exposureId)) {
                return;
            }

            requestOrder.addLast(exposureId);
            while (requestOrder.size() > MAX_REQUESTED_EXPOSURES) {
                String oldestExposureId = requestOrder.removeFirst();
                requestedExposureIds.remove(oldestExposureId);
            }

            ExposureClient.exposureStore().getOrRequest(exposureId);
        }

    }
}
