package io.github.exposure_camcorder.world.session;

public enum DynamicCaptureSessionEndReason {
    RELEASED,
    TIME_LIMIT,
    FILM_EXHAUSTED,
    INTERRUPTED,
    PLAYER_LEFT,
    PLAYER_DIED,
    DIMENSION_CHANGED,
    INVALIDATED;

    public static DynamicCaptureSessionEndReason byName(String name) {
        for (DynamicCaptureSessionEndReason reason : values()) {
            if (reason.name().equals(name)) {
                return reason;
            }
        }
        return INTERRUPTED;
    }
}
