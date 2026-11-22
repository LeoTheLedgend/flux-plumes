package dev.leo.fluxplumes.client;

import dev.alyti.fluxnautics.client.render.ThrusterCompartmentCache;
import dev.leo.fluxplumes.PlumeStyle;
import dev.leo.fluxplumes.effect.PlumeMath;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Objects;

/**
 * Our own trail: soft glowing dots laid along the path the nozzle travelled, spaced by distance so it is smooth at any
 * speed and never wobbles. The afterburner leaves a thin white-hot-to-blue streak, the rocket a warm glow that cools.
 * Render thread only.
 */
public final class PlumeTrail {
    private static final Int2ObjectOpenHashMap<double[]> LAST = new Int2ObjectOpenHashMap<>();
    private static final int MAX_PER_FRAME = 24;

    private PlumeTrail() {}

    public static void emit(ThrusterCompartmentCache.Compartment compartment, Matrix4f matrix,
                            float length, float span, PlumeContext ctx, float heat) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level) || mc.isPaused()) {
            return;
        }
        int key = Objects.hash(compartment.anchor.pos, compartment.groupId);
        float thrust = Mth.clamp(length / Math.max(span, 1f), 0f, 2f);
        float[] b = PlumeMotion.bounds(compartment);
        if (!ctx.trail() || b == null || thrust < 0.1f) {
            LAST.remove(key);
            return;
        }

        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        Vector3f nozzle = new Vector3f(b[0], b[1], 0f).mulPosition(matrix);
        double wx = cam.x + nozzle.x;
        double wy = cam.y + nozzle.y;
        double wz = cam.z + nozzle.z;
        double now = System.nanoTime() / 1.0e9;

        double[] last = LAST.get(key);
        if (last == null || now - last[3] > 0.3) {
            LAST.put(key, new double[]{wx, wy, wz, now});
            return;
        }
        double dx = wx - last[0], dy = wy - last[1], dz = wz - last[2];
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist > 64.0) { // teleported
            LAST.put(key, new double[]{wx, wy, wz, now});
            return;
        }

        boolean rocket = !PlumeContext.usesDiamonds(ctx.style(), ctx.shape());
        boolean ion = false;
        boolean aurora = false;
        float radius = Math.max(b[2], 0.4f);
        float lifeScale = 0.8f + 0.4f * Math.min(radius, 3f);
        float h = rocket ? 1f : Mth.clamp(heat, 0f, 1f);
        double spacing = ion ? 0.18 : aurora ? 0.45 : rocket ? 0.6 : 0.3;
        int n = Math.min(MAX_PER_FRAME, (int) (dist / spacing));
        last[3] = now;
        if (n <= 0) {
            return;
        }

        // Colours: afterburner starts white-hot and cools through the body colour to the edge colour.
        int hotStart = PlumeMath.lerpColor(0xFFF2D0, ctx.core(), h);
        int hotMid = PlumeMath.lerpColor(0xFF9A2E, ctx.mid(), h);
        int hotEnd = PlumeMath.lerpColor(0xB03010, ctx.outer(), h);
        int start = ion ? ctx.core() : aurora ? ctx.accent() : rocket ? ctx.mid() : PlumeMath.lerpColor(hotStart, hotMid, 0.5f);
        int end = ion ? ctx.outer() : aurora ? ctx.mid() : rocket ? ctx.accent() : hotEnd;

        float size = ion ? radius * 0.16f : aurora ? radius * 0.9f : rocket ? radius * 0.7f : radius * 0.35f;
        float endScale = ion ? 0.12f : aurora ? 2.5f : rocket ? 1.8f : 0.3f;
        int life = (int) ((ion ? 14 : aurora ? 48 : rocket ? 36 : 22) * lifeScale);
        float alpha = (ion ? 0.8f : aurora ? 0.28f : rocket ? 0.35f : 0.55f) * ctx.opacity() * Math.min(thrust, 1f);

        double ux = dx / dist, uy = dy / dist, uz = dz / dist;
        for (int i = 1; i <= n; i++) {
            double d = i * spacing;
            mc.particleEngine.add(new TrailParticle(level,
                last[0] + ux * d, last[1] + uy * d, last[2] + uz * d,
                start, end, size, endScale, life, alpha));
        }
        // Carry the remainder so spacing stays even between frames.
        double adv = n * spacing;
        last[0] += ux * adv;
        last[1] += uy * adv;
        last[2] += uz * adv;
    }
}

