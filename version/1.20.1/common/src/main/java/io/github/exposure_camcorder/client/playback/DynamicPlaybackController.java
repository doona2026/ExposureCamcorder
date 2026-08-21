package io.github.exposure_camcorder.client.playback;

import io.github.exposure_camcorder.world.component.DynamicPhotographSettings;
import io.github.exposure_camcorder.world.component.DynamicPhotographSummary;

public class DynamicPlaybackController {
    public DynamicPlaybackSession createSession(DynamicPhotographSummary summary, DynamicPhotographSettings settings) {
        return new DynamicPlaybackSession(summary.frameCount(), settings.defaultPlaybackTicksPerFrame(), settings.loop());
    }

    public boolean tick(DynamicPlaybackSession session) {
        if (!session.canAnimate() || session.paused()) {
            return false;
        }

        session.advanceProgress();
        boolean advanced = false;
        while (session.shouldAdvanceFrame()) {
            session.consumeFrameAdvance();
            if (!advanceToNextFrame(session)) {
                return advanced;
            }
            advanced = true;
        }
        return advanced;
    }

    public boolean advanceToNextFrame(DynamicPlaybackSession session) {
        if (!session.canAnimate()) {
            return false;
        }

        int nextFrameIndex = session.currentFrameIndex() + 1;
        if (nextFrameIndex < session.frameCount()) {
            session.setCurrentFrameIndex(nextFrameIndex);
            return true;
        }

        session.setCurrentFrameIndex(0);
        session.pause();
        return false;
    }

    public boolean advanceToPreviousFrame(DynamicPlaybackSession session) {
        if (!session.canAnimate()) {
            return false;
        }

        int previousFrameIndex = session.currentFrameIndex() - 1;
        if (previousFrameIndex >= 0) {
            session.setCurrentFrameIndex(previousFrameIndex);
            return true;
        }

        if (session.loop()) {
            session.setCurrentFrameIndex(session.frameCount() - 1);
            return true;
        }

        session.setCurrentFrameIndex(0);
        return false;
    }

    public void jumpToFrame(DynamicPlaybackSession session, int frameIndex) {
        session.setCurrentFrameIndex(frameIndex);
    }
}
