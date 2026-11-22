package dev.leo.fluxplumes.client;

import dev.alyti.fluxnautics.client.render.ThrusterCompartmentCache;
import foundry.veil.api.client.render.VeilRenderSystem;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Objects;

/**
 * Feeds the heat haze post pipeline with where our plumes are on screen.
 * During render each compartment records its nozzle's world position and a world-space radius;
 * the post event projects those to screen space and distorts the background there.
 * Render thread only.
 */
public final class PlumeHaze {
    public static final ResourceLocation PIPELINE = ResourceLocation.fromNamespaceAndPath("fluxplumes", "heat_haze");
    private static final int MAX = 8;

    private static final Int2ObjectOpenHashMap<double[]> PLUMES = new Int2ObjectOpenHashMap<>();
    private static boolean enabled;

    private PlumeHaze() {}

    /** Marks the pipeline active; safe to call every tick, it only registers once. */
    public static void enable() {
        if (enabled) {
            return;
        }
        enabled = true;
        var manager = VeilRenderSystem.renderer().getPostProcessingManager();
        if (manager != null) {
            manager.add(PIPELINE);
        }
    }

    /** Wipe the per-frame list; called at the end of post processing. */
    public static void clear() {
        PLUMES.clear();
    }

    public static int count() {
        return Math.min(PLUMES.size(), MAX);
    }

    public static double[] get(int i) {
        if (i < 0 || i >= PLUMES.size()) {
            return null;
        }
        int k = 0;
        for (double[] v : PLUMES.values()) {
            if (k == i) {
                return v;
            }
            k++;
        }
        return null;
    }

    /** Record one compartment's plume with full opacity (used by styles with no PlumeContext, e.g. DEFAULT). */
    public static void record(ThrusterCompartmentCache.Compartment compartment, Matrix4f matrix,
                              float length, float span) {
        record(compartment, matrix, length, span, null, 1f);
    }

    /** Record one compartment's plume: {worldX, worldY, worldZ, worldRadius, strength}. */
    public static void record(ThrusterCompartmentCache.Compartment compartment, Matrix4f matrix,
                              float length, float span, PlumeContext ctx) {
        record(compartment, matrix, length, span, ctx, ctx != null ? ctx.opacity() : 1f);
    }

    private static void record(ThrusterCompartmentCache.Compartment compartment, Matrix4f matrix,
                               float length, float span, PlumeContext ctx, float opacity) {
        enable();
        if (PLUMES.size() >= MAX) {
            return;
        }
        float[] b = PlumeMotion.bounds(compartment);
        if (b == null) {
            return;
        }
        float thrust = Mth.clamp(length / Math.max(span, 1f), 0f, 3f);
        if (thrust < 0.1f) {
            return;
        }
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vector3f nozzle = new Vector3f(b[0], b[1], 0f).mulPosition(matrix);
        double wx = cam.x + nozzle.x;
        double wy = cam.y + nozzle.y;
        double wz = cam.z + nozzle.z;
        double radius = Math.max(b[2], 0.4f) * (1.0 + 0.35 * Math.min(thrust, 1.5f));
        float strength = (0.6f + 0.4f * Math.min(thrust, 1.5f)) * opacity;
        PLUMES.put(Objects.hash(compartment.anchor.pos, compartment.groupId),
                new double[]{wx, wy, wz, radius, strength});
    }
}
