package io.github.exposure_camcorder.world.item;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum DynamicMode implements StringRepresentable {
    REGULAR("regular"),
    DYNAMIC("dynamic");

    public static final Codec<DynamicMode> CODEC = StringRepresentable.fromEnum(DynamicMode::values);
    public static final StreamCodec<RegistryFriendlyByteBuf, DynamicMode> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public DynamicMode decode(RegistryFriendlyByteBuf buffer) {
            return byName(buffer.readUtf());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, DynamicMode value) {
            buffer.writeUtf(value.getSerializedName());
        }
    };

    private final String serializedName;

    DynamicMode(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public DynamicMode next() {
        return this == REGULAR ? DYNAMIC : REGULAR;
    }

    public static DynamicMode byName(String name) {
        for (DynamicMode mode : values()) {
            if (mode.serializedName.equals(name)) {
                return mode;
            }
        }
        return REGULAR;
    }
}
