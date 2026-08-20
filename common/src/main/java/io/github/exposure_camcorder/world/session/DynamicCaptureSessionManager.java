package io.github.exposure_camcorder.world.session;

import io.github.mortuusars.exposure.world.camera.frame.Frame;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class DynamicCaptureSessionManager {
    private final Map<UUID, DynamicCaptureSession> sessionsByPlayer = new LinkedHashMap<>();

    public DynamicCaptureSession startSession(UUID playerId, String sessionId, long startTick, int captureIntervalTicks,
                                              int maxFrames, int maxRecordingDurationTicks) {
        DynamicCaptureSession existing = sessionsByPlayer.get(playerId);
        if (existing != null && existing.isActive()) {
            throw new IllegalStateException("Player already has an active dynamic capture session.");
        }

        DynamicCaptureSession session = new DynamicCaptureSession(sessionId, playerId, startTick, captureIntervalTicks,
                maxFrames, maxRecordingDurationTicks);
        sessionsByPlayer.put(playerId, session);
        return session;
    }

    public Optional<DynamicCaptureSession> getActiveSession(UUID playerId) {
        DynamicCaptureSession session = sessionsByPlayer.get(playerId);
        return session != null && session.isActive() ? Optional.of(session) : Optional.empty();
    }

    public Collection<DynamicCaptureSession> getActiveSessions() {
        return sessionsByPlayer.values().stream().filter(DynamicCaptureSession::isActive).toList();
    }

    public void appendFrame(UUID playerId, Frame frame) {
        DynamicCaptureSession session = requireActiveSession(playerId);
        session.appendFrame(frame);
    }

    public void requestStop(UUID playerId, DynamicCaptureSessionEndReason reason) {
        requireActiveSession(playerId).requestStop(reason);
    }

    public DynamicCaptureSessionResult finishSession(UUID playerId, DynamicCaptureSessionEndReason fallbackReason) {
        DynamicCaptureSession session = requireActiveSession(playerId);
        DynamicCaptureSessionResult result = session.finish(fallbackReason);
        sessionsByPlayer.remove(playerId);
        return result;
    }

    public void removeSession(UUID playerId) {
        sessionsByPlayer.remove(playerId);
    }

    public void clear() {
        sessionsByPlayer.clear();
    }

    private DynamicCaptureSession requireActiveSession(UUID playerId) {
        return getActiveSession(playerId)
                .orElseThrow(() -> new IllegalStateException("No active session for player " + playerId));
    }
}
