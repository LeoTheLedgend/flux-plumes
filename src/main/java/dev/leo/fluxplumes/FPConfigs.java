package dev.leo.fluxplumes;

import dev.alyti.fluxnautics.config.group.GroupColorConfig;
import dev.alyti.fluxnautics.config.group.GroupConfig;
import dev.alyti.fluxnautics.config.group.GroupEnumConfig;
import mekanism.common.integration.computer.ComputerException;
import net.minecraft.nbt.CompoundTag;

import java.util.Locale;
import java.util.function.Consumer;

/** Names, defaults and parsing for the settings we add to each thruster group. */
public final class FPConfigs {
    public static final String STYLE = "plume_style";
    public static final String CUSTOM_SHAPE = "plume_custom_shape";
    public static final String MOTION = "plume_motion";
    public static final String SCALE = "plume_scale";
    public static final String MODE = "plume_color_mode";
    public static final String OPACITY = "plume_opacity";
    public static final String SHAKE = "plume_shake";
    public static final String TRAIL = "plume_trail";
    public static final String BASE = "plume_color";
    public static final String CORE = "plume_core";
    public static final String MID = "plume_mid";
    public static final String OUTER = "plume_outer";
    public static final String ACCENT = "plume_accent";

    // 0xAARRGGBB, alpha ignored by the renderer
    public static final int DEFAULT_BASE = 0xFFFF7A1A;
    public static final int DEFAULT_CORE = 0xFFFFFFFF;
    public static final int DEFAULT_MID = 0xFFFFB347;
    public static final int DEFAULT_OUTER = 0xFFFF4500;
    public static final int DEFAULT_ACCENT = 0xFFFFE9B0;

    private FPConfigs() {}

    public static boolean isOurs(String key) {
        return key.equals(STYLE) || key.equals(CUSTOM_SHAPE) || key.equals(MOTION) || key.equals(SCALE) || key.equals(MODE) || key.equals(OPACITY) || key.equals(SHAKE) || key.equals(TRAIL) || key.equals(BASE)
            || key.equals(CORE) || key.equals(MID) || key.equals(OUTER) || key.equals(ACCENT);
    }

    /** Hands every config we add to the given adder (GroupData#addConfig). */
    public static void addAll(Consumer<GroupConfig<?, ?>> add) {
        add.accept(new GroupEnumConfig<PlumeStyle>(STYLE, FPLang.STYLE, PlumeStyle.DEFAULT));
        add.accept(new GroupEnumConfig<PlumeMotionMode>(MOTION, FPLang.MOTION, PlumeMotionMode.FLOWING));
        add.accept(new GroupEnumConfig<PlumeScale>(SCALE, FPLang.SCALE, PlumeScale.STANDARD));
        add.accept(new GroupEnumConfig<PlumeColorMode>(MODE, FPLang.MODE, PlumeColorMode.SIMPLE));
        add.accept(new GroupEnumConfig<PlumeOpacity>(OPACITY, FPLang.OPACITY, PlumeOpacity.P100));
        add.accept(new GroupEnumConfig<PlumeShakeLevel>(SHAKE, FPLang.SHAKE, PlumeShakeLevel.LOW));
        add.accept(new GroupEnumConfig<PlumeTrailMode>(TRAIL, FPLang.TRAIL, PlumeTrailMode.ON));
        add.accept(new GroupColorConfig(BASE, FPLang.BASE, DEFAULT_BASE));
        add.accept(new GroupColorConfig(CORE, FPLang.CORE, DEFAULT_CORE));
        add.accept(new GroupColorConfig(MID, FPLang.MID, DEFAULT_MID));
        add.accept(new GroupColorConfig(OUTER, FPLang.OUTER, DEFAULT_OUTER));
        add.accept(new GroupColorConfig(ACCENT, FPLang.ACCENT, DEFAULT_ACCENT));
    }

    /** ComputerCraft input -> the same NBT format the configs read and write. */
    public static void parseInto(CompoundTag tag, String key, Object value) throws ComputerException {
        if (key.equals(STYLE)) {
            tag.putString(key, parseEnum(key, value, PlumeStyle.class).name());
        } else if (key.equals(CUSTOM_SHAPE)) {
            tag.putString(key, parseEnum(key, value, PlumeShape.class).name());
        } else if (key.equals(MOTION)) {
            tag.putString(key, parseEnum(key, value, PlumeMotionMode.class).name());
        } else if (key.equals(SCALE)) {
            tag.putString(key, parseEnum(key, value, PlumeScale.class).name());
        } else if (key.equals(MODE)) {
            tag.putString(key, parseEnum(key, value, PlumeColorMode.class).name());
        } else if (key.equals(TRAIL)) {
            tag.putString(key, parseEnum(key, value, PlumeTrailMode.class).name());
        } else if (key.equals(SHAKE)) {
            tag.putString(key, parseEnum(key, value, PlumeShakeLevel.class).name());
        } else if (key.equals(OPACITY)) {
            tag.putString(key, parseEnum(key, value, PlumeOpacity.class).name());
        } else {
            tag.putInt(key, parseColor(key, value));
        }
    }

    private static <E extends Enum<E>> E parseEnum(String key, Object value, Class<E> type) throws ComputerException {
        if (value instanceof String s) {
            try {
                return Enum.valueOf(type, s.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // fall through
            }
        }
        throw new ComputerException("%s must be one of: %s", key, java.util.Arrays.toString(type.getEnumConstants()));
    }

    private static int parseColor(String key, Object value) throws ComputerException {
        if (value instanceof String s && s.length() == 6) {
            try {
                return 0xFF000000 | Integer.parseInt(s, 16);
            } catch (NumberFormatException ignored) {
                // fall through
            }
        } else if (value instanceof Double d) {
            return 0xFF000000 | (d.intValue() & 0xFFFFFF);
        }
        throw new ComputerException("%s must be a 6 digit hex string like \"FF8800\" or a number", key);
    }
}
