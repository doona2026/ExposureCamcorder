package io.github.exposure_camcorder.world.component;

import com.mojang.serialization.Codec;

public record DynamicSessionId(String value) {
    public static final DynamicSessionId EMPTY = new DynamicSessionId("");
    public static final Codec<DynamicSessionId> CODEC = Codec.STRING.xmap(DynamicSessionId::new, DynamicSessionId::value);
}
