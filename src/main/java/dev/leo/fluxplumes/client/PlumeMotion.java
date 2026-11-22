package dev.leo.fluxplumes.client;

import dev.alyti.fluxnautics.client.render.ThrusterCompartmentCache;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.Objects;

/**
 * Makes the plume follow the path the nozzle recently travelled.
 * <p>
 * Each nozzle's recent world positions and exhaust directions are recorded. Gas that left the nozzle {@code a}
 * seconds ago is where the nozzle was then, carried by the velocity it had then, plus the exhaust push. The
 * difference between that and where the rigid (straight) plume would put the same point is an offset. We compute
 * that offset at {@link #K} points along the plume and the tessellation shader bends the plume by it, so a ship
 * that turns or swerves leaves a curved plume like the real trail. Steady flight gives zero offset.
 * Render thread only.
 */
public final class PlumeMotion {
    public static final int K = 12;

    /** Offsets (x,y,z per point, world axes) along the plume, uploaded as {@code uFPBend[K]}. */
    public static final float[] curve = new float[K * 3];

    /** Unused now, kept so older snippets still compile. */
    public static final Vector3f bend = new Vector3f();

    /** How far back in time the tip of the plume reaches, in seconds. */
    private static final double WINDOW = 0.8;
    /** Air drag on the exhaust: how fast gas that left the nozzle slows to rest in the air, in seconds. Lower = bends more. */
    private static final double DRAG_TAU = 0.7;
    /** Overall bend strength. 1 = physical, lower = subtler. */
    private static final double STRENGTH = 1.0;
    private static final double KEEP = 1.2;
    private static final double MIN_STEP = 0.025;
    private static final double VEL_H = 0.15;
    /** Low-pass time for the bend, in seconds, so it never jitters. */
    private static final double SMOOTH = 0.18;
    private static final int MAX = 64;

    private static final Int2ObjectOpenHashMap<State> STATES = new Int2ObjectOpenHashMap<>();

    private static final class State {
        final double[] t = new double[MAX];
        final double[] px = new double[MAX], py = new double[MAX], pz = new double[MAX];
        final double[] dx = new double[MAX], dy = new double[MAX], dz = new double[MAX];
        int n;
        double curT, cpx, cpy, cpz, cdx, cdy, cdz;
        double lastFrame = -1;
        double prevCompute = -1;
        final float[] out = new float[K * 3];

        void reset() {
            n = 0;
            lastFrame = -1;
            prevCompute = -1;
            Arrays.fill(out, 0f);
        }

        void push(double time, double x, double y, double z, double ax, double ay, double az) {
            if (n == MAX) {
                System.arraycopy(t, 1, t, 0, MAX - 1);
                System.arraycopy(px, 1, px, 0, MAX - 1);
                System.arraycopy(py, 1, py, 0, MAX - 1);
                System.arraycopy(pz, 1, pz, 0, MAX - 1);
                System.arraycopy(dx, 1, dx, 0, MAX - 1);
                System.arraycopy(dy, 1, dy, 0, MAX - 1);
                System.arraycopy(dz, 1, dz, 0, MAX - 1);
                n--;
            }
            t[n] = time; px[n] = x; py[n] = y; pz[n] = z; dx[n] = ax; dy[n] = ay; dz[n] = az;
            n++;
        }

        void dropOlderThan(double time) {
            int k = 0;
            while (k < n - 1 && t[k] < time) {
                k++;
            }
            if (k > 0) {
                int rest = n - k;
                System.arraycopy(t, k, t, 0, rest);
                System.arraycopy(px, k, px, 0, rest);
                System.arraycopy(py, k, py, 0, rest);
                System.arraycopy(pz, k, pz, 0, rest);
                System.arraycopy(dx, k, dx, 0, rest);
                System.arraycopy(dy, k, dy, 0, rest);
                System.arraycopy(dz, k, dz, 0, rest);
                n = rest;
            }
        }

        double timeAt(int i) {
            return i < n ? t[i] : curT;
        }

        /** Interpolated position (0..2) and direction (3..5) at a time. */
        void sample(double time, double[] o) {
            if (n == 0 || time >= curT) {
                o[0] = cpx; o[1] = cpy; o[2] = cpz; o[3] = cdx; o[4] = cdy; o[5] = cdz;
                return;
            }
            if (time <= t[0]) {
                o[0] = px[0]; o[1] = py[0]; o[2] = pz[0]; o[3] = dx[0]; o[4] = dy[0]; o[5] = dz[0];
                return;
            }
            int j = n; // virtual last entry is "current"
            while (j > 0 && timeAt(j - 1) > time) {
                j--;
            }
            int a = j - 1;
            int b = j;
            double ta = timeAt(a);
            double tb = timeAt(b);
            double f = tb > ta ? (time - ta) / (tb - ta) : 0.0;
            double ax = a < n ? px[a] : cpx, ay = a < n ? py[a] : cpy, az = a < n ? pz[a] : cpz;
            double bx = b < n ? px[b] : cpx, by = b < n ? py[b] : cpy, bz = b < n ? pz[b] : cpz;
            o[0] = ax + (bx - ax) * f;
            o[1] = ay + (by - ay) * f;
            o[2] = az + (bz - az) * f;
            double adx = a < n ? dx[a] : cdx, ady = a < n ? dy[a] : cdy, adz = a < n ? dz[a] : cdz;
            double bdx = b < n ? dx[b] : cdx, bdy = b < n ? dy[b] : cdy, bdz = b < n ? dz[b] : cdz;
            double ex = adx + (bdx - adx) * f, ey = ady + (bdy - ady) * f, ez = adz + (bdz - adz) * f;
            double len = Math.sqrt(ex * ex + ey * ey + ez * ez);
            if (len > 1.0e-6) {
                ex /= len; ey /= len; ez /= len;
            }
            o[3] = ex; o[4] = ey; o[5] = ez;
        }
    }

    private static final double[] SA = new double[6];
    private static final double[] SB = new double[6];
    private static final double[] SC = new double[6];

    private PlumeMotion() {}

    public static void update(ThrusterCompartmentCache.Compartment compartment, Matrix4f matrix, float length) {
        update(compartment, matrix, length, 1f);
    }

    public static void update(ThrusterCompartmentCache.Compartment compartment, Matrix4f matrix, float length, float strength) {
        Arrays.fill(curve, 0f);
        float[] b = bounds(compartment);
        if (b == null) {
            return;
        }
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vector3f nozzle = new Vector3f(b[0], b[1], 0f).mulPosition(matrix);
        Vector3f dir = new Vector3f(0f, 0f, 1f).mulDirection(matrix).normalize();
        double wx = cam.x + nozzle.x;
        double wy = cam.y + nozzle.y;
        double wz = cam.z + nozzle.z;

        State s = STATES.computeIfAbsent(Objects.hash(compartment.anchor.pos, compartment.groupId), k -> new State());
        double now = System.nanoTime() / 1.0e9;

        if (s.lastFrame < 0 || now - s.lastFrame > 0.5) {
            s.reset(); // first time, or it was out of view for a while
        }
        // Several layers render per frame; only advance the history once per frame.
        if (s.lastFrame < 0 || now - s.lastFrame >= 0.002) {
            s.lastFrame = now;
            s.curT = now;
            s.cpx = wx; s.cpy = wy; s.cpz = wz;
            s.cdx = dir.x; s.cdy = dir.y; s.cdz = dir.z;
            if (s.n == 0 || now - s.t[s.n - 1] >= MIN_STEP) {
                s.push(now, wx, wy, wz, dir.x, dir.y, dir.z);
            }
            s.dropOlderThan(now - KEEP);
            compute(s, now, Math.max(length, 1f), strength);
        }
        System.arraycopy(s.out, 0, curve, 0, K * 3);
    }

    private static void compute(State s, double now, float length, float strength) {
        double dtc = s.prevCompute < 0 ? 1.0 : now - s.prevCompute;
        s.prevCompute = now;
        float al = (float) (1.0 - Math.exp(-dtc / SMOOTH));
        // Gas leaves the nozzle with the ship's velocity plus the exhaust push, then air drag slows it to rest:
        // it travels (v + exhaust) * tau * (1 - e^(-a/tau)) in the `a` seconds after leaving. Choosing the push so a
        // stationary plume is exactly straight and length `length`, the offset from the rigid plume at point lp is
        //   (where the nozzle was then - where it is now)      <- the trail path
        // + k * tau * lp * (ship velocity then)                 <- velocity it carried
        // + length * lp * (exhaust direction then - now)        <- how the nozzle has turned
        // with a(lp) = -tau * ln(1 - k * lp) and k = 1 - e^(-WINDOW/tau). Steady straight flight still bends it back
        // toward the trail, while a stationary ship gives zero.
        double k = 1.0 - Math.exp(-WINDOW / DRAG_TAU);
        float limit = 0.9f * length;
        for (int i = 0; i < K; i++) {
            float lp = i / (float) (K - 1);
            double a = -DRAG_TAU * Math.log(1.0 - k * lp);
            double target = now - a;

            s.sample(target, SA);
            s.sample(Math.min(now, target + VEL_H), SB);
            s.sample(target - VEL_H, SC);
            double h = Math.min(now, target + VEL_H) - Math.max(target - VEL_H, now - KEEP);
            double vx = 0, vy = 0, vz = 0;
            if (h > 1.0e-3) {
                vx = (SB[0] - SC[0]) / h;
                vy = (SB[1] - SC[1]) / h;
                vz = (SB[2] - SC[2]) / h;
            }
            double carry = k * DRAG_TAU * lp;
            double ox = (SA[0] - s.cpx) + carry * vx + length * lp * (SA[3] - s.cdx);
            double oy = (SA[1] - s.cpy) + carry * vy + length * lp * (SA[4] - s.cdy);
            double oz = (SA[2] - s.cpz) + carry * vz + length * lp * (SA[5] - s.cdz);
            ox *= strength; oy *= strength; oz *= strength;

            float fx = (float) ox, fy = (float) oy, fz = (float) oz;
            float len = (float) Math.sqrt(fx * fx + fy * fy + fz * fz);
            if (len > limit && len > 1.0e-4f) {
                float c = limit / len;
                fx *= c; fy *= c; fz *= c;
            }
            s.out[i * 3] += (fx - s.out[i * 3]) * al;
            s.out[i * 3 + 1] += (fy - s.out[i * 3 + 1]) * al;
            s.out[i * 3 + 2] += (fz - s.out[i * 3 + 2]) * al;
        }
    }

    /** {centerX, centerY, radius} of the plume cross-section, same maths as Fluxnautics' renderWalls. */
    public static float[] bounds(ThrusterCompartmentCache.Compartment compartment) {
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
            return null;
        }
        float cx = (minX + maxX) / 2f;
        float cy = (minY + maxY) / 2f;
        return new float[]{cx, cy, Math.max(Math.max(cx - minX, maxX - cx), Math.max(cy - minY, maxY - cy))};
    }
}
