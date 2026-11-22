package dev.leo.fluxplumes.effect;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;
import java.util.function.IntFunction;

public enum PlumeType implements StringRepresentable {
    /** Leave Fluxnautics' own effect alone. */
    NONE,
    /** Flame + smoke, with ground-hugging smoke when the exhaust points at nearby terrain. */
    ROCKET,
    /** Narrow afterburner plume with shock diamonds. */
    SR71;

    public static final Codec<PlumeType> CODEC = StringRepresentable.fromEnum(PlumeType::values);
    public static final IntFunction<PlumeType> BY_ID =
            ByIdMap.continuous(PlumeType::ordinal, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
    public static final StreamCodec<ByteBuf, PlumeType> STREAM_CODEC =
            ByteBufCodecs.idMapper(BY_ID, PlumeType::ordinal);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component label() {
        return Component.translatable("gui.fluxplumes.plume." + getSerializedName());
    }
}
