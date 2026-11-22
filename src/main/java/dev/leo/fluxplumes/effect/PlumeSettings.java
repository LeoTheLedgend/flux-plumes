package dev.leo.fluxplumes.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Effect choice plus a four-slot palette (0xRRGGBB ints):
 * core = hottest centre, mid = main flame body, outer = edge/afterburner glow,
 * accent = smoke tint (rocket) or shock-diamond colour (SR-71).
 */
public record PlumeSettings(PlumeType type, int core, int mid, int outer, int accent) {

    public static final PlumeSettings DEFAULT = new PlumeSettings(PlumeType.NONE, 0xFFFFFF, 0xFFB347, 0xFF4500, 0x808080);

    public static final MapCodec<PlumeSettings> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            PlumeType.CODEC.fieldOf("type").forGetter(PlumeSettings::type),
            Codec.INT.fieldOf("core").forGetter(PlumeSettings::core),
            Codec.INT.fieldOf("mid").forGetter(PlumeSettings::mid),
            Codec.INT.fieldOf("outer").forGetter(PlumeSettings::outer),
            Codec.INT.fieldOf("accent").forGetter(PlumeSettings::accent)
    ).apply(i, PlumeSettings::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlumeSettings> STREAM_CODEC = StreamCodec.composite(
            PlumeType.STREAM_CODEC, PlumeSettings::type,
            ByteBufCodecs.INT, PlumeSettings::core,
            ByteBufCodecs.INT, PlumeSettings::mid,
            ByteBufCodecs.INT, PlumeSettings::outer,
            ByteBufCodecs.INT, PlumeSettings::accent,
            PlumeSettings::new);

    /** Sensible starting palette when someone switches to a type. */
    public static PlumeSettings defaultsFor(PlumeType type) {
        return switch (type) {
            case NONE -> DEFAULT;
            case ROCKET -> new PlumeSettings(type, 0xFFF4D6, 0xFF9A2E, 0xE03A10, 0x6E6E6E);
            case SR71 -> new PlumeSettings(type, 0xFFFFFF, 0x7FB8FF, 0x2D5BFF, 0xFFE9B0);
        };
    }

    public PlumeSettings withType(PlumeType t) { return new PlumeSettings(t, core, mid, outer, accent); }
    public PlumeSettings withCore(int c)   { return new PlumeSettings(type, c & 0xFFFFFF, mid, outer, accent); }
    public PlumeSettings withMid(int c)    { return new PlumeSettings(type, core, c & 0xFFFFFF, outer, accent); }
    public PlumeSettings withOuter(int c)  { return new PlumeSettings(type, core, mid, c & 0xFFFFFF, accent); }
    public PlumeSettings withAccent(int c) { return new PlumeSettings(type, core, mid, outer, c & 0xFFFFFF); }
}
