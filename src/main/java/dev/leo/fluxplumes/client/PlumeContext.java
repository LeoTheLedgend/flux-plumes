package dev.leo.fluxplumes.client;

import dev.alyti.fluxnautics.client.render.ThrusterCompartmentCache;
import dev.alyti.fluxnautics.config.group.GroupConfig;
import dev.alyti.fluxnautics.multiblock.ThrusterArrayMultiblockData;
import dev.leo.fluxplumes.FPConfigs;
import dev.leo.fluxplumes.FluxPlumes;
import dev.leo.fluxplumes.PlumeColorMode;
import dev.leo.fluxplumes.PlumeOpacity;
import dev.leo.fluxplumes.PlumeShakeLevel;
import dev.leo.fluxplumes.PlumeTrailMode;
import dev.leo.fluxplumes.PlumeStyle;
import dev.leo.fluxplumes.PlumeShape;
import dev.leo.fluxplumes.PlumeMotionMode;
import dev.leo.fluxplumes.PlumeScale;
import dev.leo.fluxplumes.effect.PlumeMath;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/** What the renderer needs to draw one compartment with one of our plume styles. Render thread only. */
public record PlumeContext(PlumeStyle style, PlumeShape shape, PlumeMotionMode motion, PlumeScale scale, int core, int mid, int outer, int accent, float ground, float opacity, float shake, boolean trail) {

    /** Set while Fluxnautics draws a compartment that uses one of our styles. */
    @Nullable
    public static PlumeContext current;

    @Nullable
    public static PlumeContext resolve(dev.alyti.fluxnautics.content.blocks.thruster.ThrusterArrayControllerBlockEntity controller,
                                       ThrusterCompartmentCache.Compartment compartment, org.joml.Vector4f groupColor) {
        ThrusterArrayMultiblockData data = controller.getMultiblock();
        if (data == null) {
            return null;
        }
        int id = compartment.groupId;
        if (id < 0 || id >= data.thrusters.groups.size()) {
            return null;
        }
        ThrusterArrayMultiblockData.GroupData group = data.thrusters.groups.get(id);
        GroupConfig<?, ?> styleConfig = group.getConfig(FPConfigs.STYLE);
        if (styleConfig == null || !(styleConfig.get() instanceof PlumeStyle style) || style == PlumeStyle.DEFAULT) {
            return null;
        }

        // Smallest positive hit distance among the thrusters in this compartment (-1 = none).
        float ground = -1f;
        for (ThrusterArrayMultiblockData.ThrusterData t : compartment.members) {
            if (t.hitDistance > 0f && (ground < 0f || t.hitDistance < ground)) {
                ground = t.hitDistance;
            }
        }

        GroupConfig<?, ?> opacityConfig = group.getConfig(FPConfigs.OPACITY);
        float opacity = opacityConfig != null && opacityConfig.get() instanceof PlumeOpacity o ? o.value() : 1f;
        GroupConfig<?, ?> shakeConfig = group.getConfig(FPConfigs.SHAKE);
        float shake = shakeConfig != null && shakeConfig.get() instanceof PlumeShakeLevel sl ? sl.multiplier() : PlumeShakeLevel.LOW.multiplier();

        GroupConfig<?, ?> trailConfig = group.getConfig(FPConfigs.TRAIL);
        boolean trail = trailConfig == null || trailConfig.get() != PlumeTrailMode.OFF;

        GroupConfig<?, ?> modeConfig = group.getConfig(FPConfigs.MODE);
        boolean simple = modeConfig == null || modeConfig.get() != PlumeColorMode.ADVANCED;
        if (simple) {
            int base = color(group, FPConfigs.BASE, FPConfigs.DEFAULT_BASE);
            return derive(style, shape(group), motion(group), scale(group), base, ground, opacity, shake, trail);
        }
        return new PlumeContext(style, shape(group), motion(group), scale(group),
            color(group, FPConfigs.CORE, FPConfigs.DEFAULT_CORE),
            color(group, FPConfigs.MID, FPConfigs.DEFAULT_MID),
            color(group, FPConfigs.OUTER, FPConfigs.DEFAULT_OUTER),
            color(group, FPConfigs.ACCENT, FPConfigs.DEFAULT_ACCENT),
            ground, opacity, shake, trail);
    }

    /** One colour in, a full palette out: white-hot core, base body, darker edge, pale diamonds/smoke. */
    private static PlumeContext derive(PlumeStyle style, PlumeShape shape, PlumeMotionMode motion, PlumeScale scale, int base, float ground, float opacity, float shake, boolean trail) {
        // Each stock profile starts with a recognisable palette. Choosing Advanced lets players
        // overwrite every slot, which is the basis of the CUSTOM style.
        if ((base & 0xFFFFFF) == (FPConfigs.DEFAULT_BASE & 0xFFFFFF)) {
            base = switch (style) {
                case FLARE -> 0xFFFFB12E;
                default -> base;
            };
        }
        int rgb = base & 0xFFFFFF;
        int core = PlumeMath.lerpColor(rgb, 0xFFFFFF, 0.15f);
        int outer = PlumeMath.lerpColor(rgb, 0x000000, 0.45f);
        int accent = PlumeMath.lerpColor(rgb, 0xFFFFFF, usesDiamonds(style, shape) ? 0.6f : 0.9f);
        return new PlumeContext(style, shape, motion, scale, core, rgb, outer, accent, ground, opacity, shake, trail);
    }

    private static PlumeShape shape(ThrusterArrayMultiblockData.GroupData group) {
        GroupConfig<?, ?> config = group.getConfig(FPConfigs.CUSTOM_SHAPE);
        return config != null && config.get() instanceof PlumeShape shape ? shape : PlumeShape.FLAME;
    }

    private static PlumeMotionMode motion(ThrusterArrayMultiblockData.GroupData group) {
        GroupConfig<?, ?> config = group.getConfig(FPConfigs.MOTION);
        return config != null && config.get() instanceof PlumeMotionMode value ? value : PlumeMotionMode.FLOWING;
    }

    private static PlumeScale scale(ThrusterArrayMultiblockData.GroupData group) {
        GroupConfig<?, ?> config = group.getConfig(FPConfigs.SCALE);
        return config != null && config.get() instanceof PlumeScale value ? value : PlumeScale.STANDARD;
    }

    /** SR-71 uses shock diamonds; Ion and Aurora have their own dedicated render profiles. */
    public static boolean usesDiamonds(PlumeStyle style, PlumeShape customShape) {
        return style == PlumeStyle.SR71;
    }

    private static int color(ThrusterArrayMultiblockData.GroupData group, String name, int fallback) {
        GroupConfig<?, ?> config = group.getConfig(name);
        return config != null && config.get() instanceof Integer i ? i : fallback;
    }

    @Nullable
    public RenderType renderType() {
        String path = switch (style) {
            case FLARE -> "plume_flare";
            default -> usesDiamonds(style, shape) ? "plume_sr71" : "plume_rocket";
        };
        return VeilRenderType.get(ResourceLocation.fromNamespaceAndPath(FluxPlumes.MODID, path));
    }

    public static Vector3f rgb(int argb) {
        return new Vector3f(((argb >> 16) & 0xFF) / 255f, ((argb >> 8) & 0xFF) / 255f, (argb & 0xFF) / 255f);
    }
}

