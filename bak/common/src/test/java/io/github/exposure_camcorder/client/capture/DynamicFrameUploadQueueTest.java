package io.github.exposure_camcorder.client.capture;

import io.github.exposure_camcorder.world.session.DynamicCaptureSessionEndReason;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicFrameUploadQueueTest {
    @Test
    void unacknowledgedFrameCanBeResubmittedForRetry() {
        List<Integer> submittedFrameIndices = new ArrayList<>();
        DynamicFrameUploadQueue queue = new DynamicFrameUploadQueue(
                (sessionId, frameIndex, exposureId, exposureData) -> submittedFrameIndices.add(frameIndex),
                (sessionId, reason) -> {
                });

        queue.startSession("session-a");

        assertTrue(queue.submitFrameData("session-a", 0, "exposure-0", null));
        assertTrue(queue.submitFrameData("session-a", 0, "exposure-0", null));
        assertEquals(List.of(0, 0), submittedFrameIndices);
    }

    @Test
    void acknowledgedFrameIsRejectedAfterServerStateAdvances() {
        DynamicFrameUploadQueue queue = new DynamicFrameUploadQueue(
                (sessionId, frameIndex, exposureId, exposureData) -> {
                },
                (sessionId, reason) -> {
                });

        queue.startSession("session-a");
        queue.updateState("session-a", 1);

        assertEquals(1, queue.acknowledgedFrameCount());
        assertTrue(!queue.submitFrameData("session-a", 0, "exposure-0", null));
        assertTrue(queue.submitFrameData("session-a", 1, "exposure-1", null));
    }

    @Test
    void stopRequestsUseInjectedSender() {
        List<DynamicCaptureSessionEndReason> reasons = new ArrayList<>();
        DynamicFrameUploadQueue queue = new DynamicFrameUploadQueue(
                (sessionId, frameIndex, exposureId, exposureData) -> {
                },
                (sessionId, reason) -> reasons.add(reason));

        queue.startSession("session-a");
        queue.requestStop("session-a", DynamicCaptureSessionEndReason.RELEASED);

        assertEquals(List.of(DynamicCaptureSessionEndReason.RELEASED), reasons);
    }

    @Test
    void clearDropsActiveSessionState() {
        DynamicFrameUploadQueue queue = new DynamicFrameUploadQueue(
                (sessionId, frameIndex, exposureId, exposureData) -> {
                },
                (sessionId, reason) -> {
                });

        queue.startSession("session-a");
        queue.updateState("session-a", 3);
        queue.clear();

        assertTrue(!queue.hasActiveSession());
        assertEquals(0, queue.acknowledgedFrameCount());
    }
}
