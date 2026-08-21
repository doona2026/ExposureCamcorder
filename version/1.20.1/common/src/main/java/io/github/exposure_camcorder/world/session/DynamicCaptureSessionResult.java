package io.github.exposure_camcorder.world.session;

import io.github.exposure_camcorder.world.component.DynamicPhotographFrames;
import io.github.mortuusars.exposure.world.camera.frame.Frame;

import java.util.List;

public record DynamicCaptureSessionResult(String sessionId,
                                          DynamicCaptureSessionEndReason endReason,
                                          DynamicPhotographFrames frames,
                                          boolean shouldCreatePhotograph) {
    public DynamicCaptureSessionResult(String sessionId, DynamicCaptureSessionEndReason endReason,
                                       List<Frame> frames, boolean shouldCreatePhotograph) {
        this(sessionId, endReason, new DynamicPhotographFrames(frames), shouldCreatePhotograph);
    }

    public int frameCount() {
        return frames.size();
    }
}
