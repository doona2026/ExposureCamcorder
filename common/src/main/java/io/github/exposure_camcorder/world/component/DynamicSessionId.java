package io.github.exposure_camcorder.world.component;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record DynamicSessionId(String value) {
    public static final DynamicSessionId EMPTY = new DynamicSessionId("");
    public static final Codec<DynamicSessionId> CODEC = Codec.STRING.xmap(DynamicSessionId::new, DynamicSessionId::value);
    public static final StreamCodec<RegistryFriendlyByteBuf, DynamicSessionId> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public DynamicSessionId decode(RegistryFriendlyByteBuf buffer) {
            return new DynamicSessionId(buffer.readUtf());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, DynamicSessionId value) {
            buffer.writeUtf(value.value());
        }
    };
}
