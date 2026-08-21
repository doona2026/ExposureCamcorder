package io.github.exposure_camcorder.client.playback;

import io.github.exposure_camcorder.ExposureCamcorder;
import io.github.exposure_camcorder.client.gui.screen.DynamicPhotographScreenController;
import io.github.exposure_camcorder.client.gui.screen.DynamicPhotographViewModel;
import io.github.exposure_camcorder.client.render.DynamicPhotographPrefetchPlanner;
import io.github.exposure_camcorder.compatibility.exposure.ExposurePhotographScreenHooks;
import io.github.exposure_camcorder.world.component.DynamicPhotographFrames;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.world.camera.frame.Frame;
import io.github.mortuusars.exposure.world.entity.PhotographFrameEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;

public final class DynamicPhotographDisplayPlaybackManager {
    private static final DynamicPhotographDisplayPlaybackManager INSTANCE = new DynamicPhotographDisplayPlaybackManager();
    private static final DynamicPhotographPrefetchPlanner PREFETCH_PLANNER = new DynamicPhotographPrefetchPlanner(2, 8);
    private static final int MAX_REQUESTED_EXPOSURES = 48;

    private final WeakHashMap<Object, PlaybackState> slotStates = new WeakHashMap<>();
    private final Map<Integer, PlaybackState> frameStates = new HashMap<>();

    private WeakReference<Object> hoveredSlot = new WeakReference<>(null);
    private Screen hoveredSlotScreen;

    private DynamicPhotographDisplayPlaybackManager() {
    }

    public static DynamicPhotographDisplayPlaybackManager getInstance() {
        return INSTANCE;
    }

    public void tick(Minecraft minecraft) {
        if (minecraft.level == null) {
            reset();
            return;
        }

        if (minecraft.screen == null || minecraft.screen != hoveredSlotScreen) {
            clearHoveredSlot();
        }

        frameStates.entrySet().removeIf(entry -> minecraft.level.getEntity(entry.getKey()) == null);

        slotStates.values().forEach(PlaybackState::tick);
        frameStates.values().forEach(PlaybackState::tick);
    }

    public void reset() {
        slotStates.clear();
        frameStates.clear();
        clearHoveredSlot();
    }

    public void trackSlotHover(Screen screen, Object slotWidget, ItemStack stack, boolean hovered) {
        if (!ExposurePhotographScreenHooks.isDynamicPhotograph(stack)) {
            if (hoveredSlot.get() == slotWidget) {
                clearHoveredSlot();
            }
            return;
        }

        PlaybackState state = slotStates.computeIfAbsent(slotWidget, key -> new PlaybackState());
        state.updateSourceStack(stack);

        if (hovered) {
            hoveredSlot = new WeakReference<>(slotWidget);
            hoveredSlotScreen = screen;
        } else if (hoveredSlot.get() == slotWidget) {
            clearHoveredSlot();
        }
    }

    public ItemStack resolveSlotDisplayStack(Object slotWidget, ItemStack stack) {
        if (!ExposurePhotographScreenHooks.isDynamicPhotograph(stack)) {
            return stack;
        }

        PlaybackState state = slotStates.computeIfAbsent(slotWidget, key -> new PlaybackState());
        state.updateSourceStack(stack);
        return state.createDisplayStack();
    }

    public ItemStack resolveFrameDisplayStack(PhotographFrameEntity entity, ItemStack stack) {
        if (!ExposurePhotographScreenHooks.isDynamicPhotograph(stack)) {
            frameStates.remove(entity.getId());
            return stack;
        }

        PlaybackState state = frameStates.computeIfAbsent(entity.getId(), key -> new PlaybackState());
        state.updateSourceStack(stack);
        return state.createDisplayStack();
    }

    public boolean togglePlayback(Minecraft minecraft) {
        PlaybackState slotState = resolveHoveredSlotState();
        if (slotState != null) {
            boolean nowPlaying = toggleState(slotState);
            showPlaybackMessage(minecraft, nowPlaying);
            ExposurePhotographScreenHooks.playControlSound();
            return true;
        }

        if (minecraft.hitResult instanceof EntityHitResult entityHitResult
                && entityHitResult.getEntity() instanceof PhotographFrameEntity frameEntity) {
            ItemStack frameItem = frameEntity.getItem();
            if (ExposurePhotographScreenHooks.isDynamicPhotograph(frameItem)) {
                PlaybackState state = frameStates.computeIfAbsent(frameEntity.getId(), key -> new PlaybackState());
                state.updateSourceStack(frameItem);
                boolean nowPlaying = toggleState(state);
                showPlaybackMessage(minecraft, nowPlaying);
                ExposurePhotographScreenHooks.playControlSound();
                return true;
            }
        }

        return false;
    }

    public Component playbackHint() {
        return Component.translatable("message.exposure_camcorder.dynamic_display.playback.hint");
    }

    private boolean toggleState(PlaybackState targetState) {
        if (targetState.playing()) {
            targetState.stop();
            return false;
        }

        slotStates.values().forEach(PlaybackState::stop);
        frameStates.values().forEach(PlaybackState::stop);
        targetState.start();
        return true;
    }

    private PlaybackState resolveHoveredSlotState() {
        Object hoveredWidget = hoveredSlot.get();
        if (hoveredWidget == null) {
            return null;
        }
        return slotStates.get(hoveredWidget);
    }

    private void clearHoveredSlot() {
        hoveredSlot = new WeakReference<>(null);
        hoveredSlotScreen = null;
    }

    private void showPlaybackMessage(Minecraft minecraft, boolean playing) {
        minecraft.gui.setOverlayMessage(Component.translatable(playing
                ? "message.exposure_camcorder.dynamic_display.playback.started"
                : "message.exposure_camcorder.dynamic_display.playback.stopped"), false);
    }

    private static ItemStack createDisplayPhotographStack(Frame frame) {
        ItemStack displayStack = new ItemStack(Exposure.Items.PHOTOGRAPH.get());
        if (frame != Frame.EMPTY) {
            io.github.mortuusars.exposure.Exposure.DataComponents.setPhotographFrame(displayStack, frame);
            io.github.mortuusars.exposure.Exposure.DataComponents.setPhotographType(displayStack, frame.type());
        }
        return displayStack;
    }

    private static final class PlaybackState {
        private final DynamicPhotographScreenController controller = new DynamicPhotographScreenController();
        private final Set<String> requestedExposureIds = new HashSet<>();
        private final ArrayDeque<String> requestOrder = new ArrayDeque<>();
        private ItemStack sourceStack = ItemStack.EMPTY;
        private Optional<Frame> lastReadyFrame = Optional.empty();
        private boolean playing;

        public void updateSourceStack(ItemStack stack) {
            if (stack.isEmpty()) {
                sourceStack = ItemStack.EMPTY;
                lastReadyFrame = Optional.empty();
                playing = false;
                return;
            }

            if (ItemStack.matches(sourceStack, stack)) {
                return;
            }

            sourceStack = stack.copy();
            controller.open(sourceStack);
            requestedExposureIds.clear();
            requestOrder.clear();
            stop();
        }

        public void tick() {
            if (!playing) {
                requestDisplayFrameWindow();
                return;
            }

            controller.tick();
            if (controller.playbackSession().paused()) {
                stop();
            }
            requestDisplayFrameWindow();
        }

        public boolean playing() {
            return playing;
        }

        public void start() {
            if (!ExposurePhotographScreenHooks.isDynamicPhotograph(sourceStack)) {
                return;
            }

            DynamicPhotographViewModel viewModel = controller.viewModel();
            controller.jumpToFrame(viewModel.coverFrameIndex());
            if (controller.playbackSession().paused()) {
                controller.togglePaused();
            }
            playing = !controller.playbackSession().paused();
            requestDisplayFrameWindow();
        }

        public void stop() {
            DynamicPhotographViewModel viewModel = controller.viewModel();
            if (!controller.playbackSession().paused()) {
                controller.togglePaused();
            }
            controller.jumpToFrame(viewModel.coverFrameIndex());
            playing = false;
            requestDisplayFrameWindow();
        }

        public ItemStack createDisplayStack() {
            Optional<Frame> frame = playing
                    ? resolveReadyDisplayFrame(controller.viewModel(), lastReadyFrame)
                    : controller.viewModel().coverFrame().filter(PlaybackState::isFrameReady);

            if (frame.isPresent()) {
                lastReadyFrame = frame;
            } else if (!playing) {
                lastReadyFrame = Optional.empty();
            }

            return createDisplayPhotographStack(frame.orElse(Frame.EMPTY));
        }

        private void requestDisplayFrameWindow() {
            DynamicPhotographFrames frames = io.github.exposure_camcorder.util.DynamicItemStackData.getDynamicPhotographFrames(sourceStack, DynamicPhotographFrames.EMPTY);
            DynamicPhotographViewModel viewModel = controller.viewModel();
            for (int frameIndex : PREFETCH_PLANNER.plan(viewModel.frameCount(), viewModel.currentFrameIndex(),
                    viewModel.coverFrameIndex())) {
                frames.getFrame(frameIndex)
                        .map(Frame::identifier)
                        .ifPresent(identifier -> identifier.ifId(this::requestExposure));
            }
        }

        private void requestExposure(String exposureId) {
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

        private static Optional<Frame> resolveReadyDisplayFrame(DynamicPhotographViewModel viewModel,
                                                                Optional<Frame> lastReadyFrame) {
            Optional<Frame> currentReadyFrame = viewModel.currentFrame().filter(PlaybackState::isFrameReady);
            if (currentReadyFrame.isPresent()) {
                return currentReadyFrame;
            }

            Optional<Frame> lastReadyLoaded = lastReadyFrame.filter(PlaybackState::isFrameReady);
            if (lastReadyLoaded.isPresent()) {
                return lastReadyLoaded;
            }

            return viewModel.coverFrame().filter(PlaybackState::isFrameReady);
        }

        private static boolean isFrameReady(Frame frame) {
            if (frame == Frame.EMPTY || frame.identifier().isEmpty()) {
                return false;
            }

            return frame.identifier().map(id -> ExposureClient.exposureStore().getOrRequest(id).getData().isPresent(),
                    texture -> true);
        }
    }
}
