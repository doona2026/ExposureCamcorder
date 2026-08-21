package io.github.exposure_camcorder.client.capture;

import io.github.exposure_camcorder.world.session.DynamicCaptureSessionEndReason;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicFrameUploadQueueTest {
    @Test
    void allowsInFlightFrameToUploadAfterStop() {
        List<Integer> uploadedFrames = new ArrayList<>();
        DynamicFrameUploadQueue queue = new DynamicFrameUploadQueue(
                (sessionId, frameIndex, exposureId, exposureData) -> uploadedFrames.add(frameIndex),
                (sessionId, reason) -> {
                });
        queue.startSession("session-a");
        assertTrue(queue.acceptFrameRequest("session-a", 0, "exposure-a"));
        queue.requestStop(DynamicCaptureSessionEndReason.RELEASED);

        assertFalse(queue.hasActiveSession());
        assertTrue(queue.submitFrameData("session-a", 0, "exposure-a", null));
        assertEquals(List.of(0), uploadedFrames);
    }

    @Test
    void doesNotUploadOlderFrameAfterStop() {
        List<Integer> uploadedFrames = new ArrayList<>();
        DynamicFrameUploadQueue queue = new DynamicFrameUploadQueue(
                (sessionId, frameIndex, exposureId, exposureData) -> uploadedFrames.add(frameIndex),
                (sessionId, reason) -> {
                });
        queue.startSession("session-a");
        queue.updateState("session-a", 1);
        assertTrue(queue.acceptFrameRequest("session-a", 1, "exposure-b"));
        queue.requestStop(DynamicCaptureSessionEndReason.RELEASED);

        assertFalse(queue.submitFrameData("session-a", 0, "exposure-a", null));
        assertTrue(uploadedFrames.isEmpty());
    }

    @Test
    void rejectsLateOlderRequestAfterNextFrameWasRequested() {
        DynamicFrameUploadQueue queue = new DynamicFrameUploadQueue(
                (sessionId, frameIndex, exposureId, exposureData) -> {
                },
                (sessionId, reason) -> {
                });
        queue.startSession("session-a");

        assertTrue(queue.acceptFrameRequest("session-a", 0, "exposure-a"));
        assertTrue(queue.acceptFrameRequest("session-a", 1, "exposure-b"));
        assertFalse(queue.acceptFrameRequest("session-a", 0, "exposure-c"));
    }

    @Test
    void dropsStaleUploadAfterRetryRequestedForSameFrame() {
        List<String> uploadedExposureIds = new ArrayList<>();
        DynamicFrameUploadQueue queue = new DynamicFrameUploadQueue(
                (sessionId, frameIndex, exposureId, exposureData) -> uploadedExposureIds.add(exposureId),
                (sessionId, reason) -> {
                });
        queue.startSession("session-a");

        assertTrue(queue.acceptFrameRequest("session-a", 0, "exposure-a"));
        assertTrue(queue.acceptFrameRequest("session-a", 0, "exposure-a-retry-1"));
        assertFalse(queue.submitFrameData("session-a", 0, "exposure-a", null));
        assertTrue(queue.submitFrameData("session-a", 0, "exposure-a-retry-1", null));
        assertEquals(List.of("exposure-a-retry-1"), uploadedExposureIds);
    }

    @Test
    void rejectsDuplicateRequestForSameFrameAndUploadId() {
        DynamicFrameUploadQueue queue = new DynamicFrameUploadQueue(
                (sessionId, frameIndex, exposureId, exposureData) -> {
                },
                (sessionId, reason) -> {
                });
        queue.startSession("session-a");

        assertTrue(queue.acceptFrameRequest("session-a", 0, "exposure-a"));
        assertFalse(queue.acceptFrameRequest("session-a", 0, "exposure-a"));
    }
}
