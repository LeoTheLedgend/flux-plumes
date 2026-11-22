package dev.leo.fluxplumes.client;

import dev.alyti.fluxnautics.client.render.ThrusterCompartmentCache;
import dev.leo.fluxplumes.FluxPlumes;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Iterator;
import java.util.Objects;

/** Shakes the camera a little when the player is close to a firing plume. Render thread only. */
@EventBusSubscriber(modid = FluxPlumes.MODID, value = Dist.CLIENT)
public final class PlumeShake {
    private static final long EXPIRE_NANOS = 300_000_000L;
    /** Largest shake in degrees at full strength. */
    private static final float MAX_DEGREES = 0.6f;

    private static final class Source {
        double x, y, z;
        float strength;
        float range;
        long nanos;
    }

    private static final Int2ObjectOpenHashMap<Source> SOURCES = new Int2ObjectOpenHashMap<>();

    private PlumeShake() {}

    public static void record(ThrusterCompartmentCache.Compartment compartment, Matrix4f matrix,
                              float length, float span, PlumeContext ctx) {
        if (ctx.shake() <= 0f) {
            return;
        }
        float[] b = PlumeMotion.bounds(compartment);
        if (b == null) {
            return;
        }
        float thrust = Mth.clamp(length / Math.max(span, 1f), 0f, 2f) / 2f;
        if (thrust < 0.05f) {
            return;
        }
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vector3f nozzle = new Vector3f(b[0], b[1], 0f).mulPosition(matrix);
        Source s = SOURCES.computeIfAbsent(Objects.hash(compartment.anchor.pos, compartment.groupId), k -> new Source());
        s.x = cam.x + nozzle.x;
        s.y = cam.y + nozzle.y;
        s.z = cam.z + nozzle.z;
        float size = Math.min(b[2], 3f);
        s.strength = thrust * (1f + 0.3f * size) * ctx.shake();
        s.range = 10f + 6f * size;
        s.nanos = System.nanoTime();
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (SOURCES.isEmpty()) {
            return;
        }
        long now = System.nanoTime();
        Vec3 cam = event.getCamera().getPosition();
        float amp = 0f;
        Iterator<Int2ObjectMap.Entry<Source>> it = SOURCES.int2ObjectEntrySet().iterator();
        while (it.hasNext()) {
            Source s = it.next().getValue();
            if (now - s.nanos > EXPIRE_NANOS) {
                it.remove();
                continue;
            }
            double dx = s.x - cam.x, dy = s.y - cam.y, dz = s.z - cam.z;
            float d = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (d < s.range) {
                float f = 1f - d / s.range;
                amp = Math.max(amp, s.strength * f * f);
            }
        }
        if (amp <= 0.001f) {
            return;
        }
        double t = now / 1.0e9;
        float deg = Math.min(amp, 2f) * MAX_DEGREES;
        float yaw = (float) (Math.sin(t * 41.0) + 0.6 * Math.sin(t * 27.3 + 1.3)) * 0.5f * deg;
        float pitch = (float) (Math.sin(t * 37.0 + 2.1) + 0.6 * Math.sin(t * 23.1 + 0.4)) * 0.5f * deg;
        float roll = (float) (Math.sin(t * 33.0 + 0.7) + 0.5 * Math.sin(t * 19.1)) * 0.8f * deg;
        event.setYaw(event.getYaw() + yaw);
        event.setPitch(event.getPitch() + pitch);
        event.setRoll(event.getRoll() + roll);
    }
}
