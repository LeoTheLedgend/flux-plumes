package dev.leo.fluxplumes.client;

import dev.alyti.fluxnautics.client.render.ThrusterCompartmentCache;
import dev.alyti.fluxnautics.content.blocks.thruster.ThrusterArrayControllerBlockEntity;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Objects;

/**
 * Emits the rocket style's smoke from the renderer, once per game tick per compartment.
 * World positions come from the plume's render matrix (camera-relative) plus the camera position,
 * so this also works for thrusters riding on a Sable sublevel.
 */
public final class PlumeSmoke {
    private static final Int2LongOpenHashMap LAST_TICK = new Int2LongOpenHashMap();

    /** Global per-tick particle budget shared across all compartments, to prevent lag. */
    private static int budget;
    private static long budgetTick = Long.MIN_VALUE;

    static {
        LAST_TICK.defaultReturnValue(-1L);
    }

    private PlumeSmoke() {}

    public static void emit(ThrusterArrayControllerBlockEntity controller,
                            ThrusterCompartmentCache.Compartment compartment,
                            Matrix4f matrix, float length, float span, PlumeContext ctx) {
        emit(controller, compartment, matrix, length, span, ctx, 1f, ctx.accent(), ctx.mid());
    }

    /** amount scales how much smoke; smokeRgb/hotRgb are the cooled and glowing puff colours. */
    public static void emit(ThrusterArrayControllerBlockEntity controller,
                            ThrusterCompartmentCache.Compartment compartment,
                            Matrix4f matrix, float length, float span, PlumeContext ctx,
                            float amount, int smokeRgb, int hotRgb) {
        if (amount <= 0.05f) {
            return;
        }
        Level raw = controller.getLevel();
        if (!(raw instanceof ClientLevel level) || Minecraft.getInstance().isPaused()) {
            return;
        }
        float thrust = Mth.clamp(length / Math.max(span, 1f), 0f, 3f);
        if (length < 0.5f || thrust < 0.05f) {
            return;
        }

        int key = Objects.hash(compartment.anchor.pos, compartment.groupId);
        long now = level.getGameTime();
        // This budget is genuinely global. Resetting it per compartment defeated the cap and made
        // large engine arrays spawn hundreds of expensive translucent particles each tick.
        if (budgetTick != now) {
            budgetTick = now;
            budget = 12;
        }
        if (budget <= 0 || LAST_TICK.get(key) == now) {
            return;
        }
        LAST_TICK.put(key, now);

        // Plume cross-section centre and radius, same maths as Fluxnautics' renderWalls.
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (ThrusterCompartmentCache.Wall wall : compartment.walls) {
            float lo = Math.min(wall.a(), wall.b());
            float hi = Math.max(wall.a(), wall.b());
            switch (wall.side()) {
                case 0, 2 -> {
                    minX = Math.min(minX, lo);
                    maxX = Math.max(maxX, hi);
                    minY = Math.min(minY, wall.c());
                    maxY = Math.max(maxY, wall.c());
                }
                default -> {
                    minX = Math.min(minX, wall.c());
                    maxX = Math.max(maxX, wall.c());
                    minY = Math.min(minY, lo);
                    maxY = Math.max(maxY, hi);
                }
            }
        }
        if (minX > maxX || minY > maxY) {
            return;
        }
        float cx = (minX + maxX) / 2f;
        float cy = (minY + maxY) / 2f;
        float radius = Math.max(Math.max(cx - minX, maxX - cx), Math.max(cy - minY, maxY - cy));
        radius = Math.max(radius, 0.4f);
        float lifetimeScale = 0.8f + 0.4f * Math.min(radius, 3f);

        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vector3f nozzle = new Vector3f(cx, cy, 0f).mulPosition(matrix);
        Vector3f dir = new Vector3f(0f, 0f, 1f).mulDirection(matrix).normalize();
        Vector3f ax = new Vector3f(1f, 0f, 0f).mulDirection(matrix).normalize();
        Vector3f ay = new Vector3f(0f, 1f, 0f).mulDirection(matrix).normalize();
        double ox = cam.x + nozzle.x;
        double oy = cam.y + nozzle.y;
        double oz = cam.z + nozzle.z;

        RandomSource rand = level.random;
        float t2 = Math.min(thrust, 2f);
        // Fewer but bigger puffs; cap lower so multiple compartments don't spam particles.
        int count = Mth.clamp(Math.round(1f + t2 * Math.min(radius, 1.25f)), 1, 3);
        count = Math.max(1, Math.round(count * amount));

        // Billows hugging the flame, hotter and tighter near the nozzle.
        for (int i = 0; i < count; i++) {
            if (budget <= 0) {
                break;
            }
            budget--;
            float t = 0.02f + 0.98f * rand.nextFloat();
            double ang = rand.nextDouble() * Math.PI * 2.0;
            float rx = (float) Math.cos(ang);
            float ry = (float) Math.sin(ang);
            float off = radius * (0.2f + 0.8f * rand.nextFloat());
            float ux = ax.x * rx + ay.x * ry;
            float uy = ax.y * rx + ay.y * ry;
            float uz = ax.z * rx + ay.z * ry;
            double sp = 0.03 + 0.02 * rand.nextDouble();
            double carry = 0.10 * thrust;
            Minecraft.getInstance().particleEngine.add(new ExhaustSmokeParticle(level,
                ox + dir.x * length * t + ux * off,
                oy + dir.y * length * t + uy * off,
                oz + dir.z * length * t + uz * off,
                dir.x * carry + ux * sp, dir.y * carry + uy * sp, dir.z * carry + uz * sp,
                smokeRgb, hotRgb, 1f - t,
                1.9f * radius + 2.1f * t * radius, 3.2f, false, ctx.opacity(), lifetimeScale));
        }

        // Big spreading cloud where the exhaust hits the ground.
        float ground = ctx.ground();
        if (ground > 0f && ground < length) {
            double ix = ox + dir.x * ground;
            double iy = oy + dir.y * ground;
            double iz = oz + dir.z * ground;
            Vector3f t1 = new Vector3f(ax);
            Vector3f t2v = new Vector3f(dir).cross(ax).normalize();
            int groundCount = Math.min(count, 3);
            for (int j = 0; j < groundCount; j++) {
                if (budget <= 0) {
                    break;
                }
                budget--;
                double ang = rand.nextDouble() * Math.PI * 2.0;
                float c = (float) Math.cos(ang);
                float s = (float) Math.sin(ang);
                float tx = t1.x * c + t2v.x * s;
                float ty = t1.y * c + t2v.y * s;
                float tz = t1.z * c + t2v.z * s;
                double speed = (0.12 + 0.28 * rand.nextDouble()) * (0.5 + 0.5 * t2);
                float spread = radius * 0.6f * rand.nextFloat();
                Minecraft.getInstance().particleEngine.add(new ExhaustSmokeParticle(level,
                    ix + tx * spread - dir.x * 0.3,
                    iy + ty * spread - dir.y * 0.3,
                    iz + tz * spread - dir.z * 0.3,
                    tx * speed, ty * speed, tz * speed,
                    smokeRgb, hotRgb, 0.9f,
                    1.5f * radius + 0.8f, 4.0f, true, ctx.opacity(), lifetimeScale));
            }
        }
    }
}
