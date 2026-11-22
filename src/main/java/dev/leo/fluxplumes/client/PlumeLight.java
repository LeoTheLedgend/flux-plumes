package dev.leo.fluxplumes.client;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.light.data.PointLightData;
import foundry.veil.api.client.render.light.renderer.LightRenderHandle;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Colored dynamic point light emitted from each firing plume compartment. */
public final class PlumeLight {
    private static final Int2ObjectOpenHashMap<LightRenderHandle<PointLightData>> ACTIVE = new Int2ObjectOpenHashMap<>();
    private static final it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap LAST_SEEN = new it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap();

    static {
        LAST_SEEN.defaultReturnValue(-1L);
    }

    private PlumeLight() {}

    /** Call each frame while the compartment is firing; removes light when active=false. */
    public static void update(int key, Vec3 pos, Vector3f color, float radius, float brightness, boolean active) {
        var renderer = VeilRenderSystem.renderer().getLightRenderer();
        if (renderer == null) {
            return;
        }
        if (!active) {
            var handle = ACTIVE.remove(key);
            if (handle != null) {
                handle.free();
            }
            LAST_SEEN.remove(key);
            return;
        }
        LAST_SEEN.put(key, System.nanoTime());
        var handle = ACTIVE.get(key);
        if (handle == null || !handle.isValid()) {
            var light = new PointLightData();
            light.setPosition(pos.x, pos.y, pos.z);
            light.setRadius(radius);
            light.setBrightness(brightness);
            light.setColor(color.x, color.y, color.z);
            handle = renderer.addLight(light);
            ACTIVE.put(key, handle);
        } else {
            var light = handle.getLightData();
            light.setPosition(pos.x, pos.y, pos.z);
            light.setRadius(radius);
            light.setBrightness(brightness);
            light.setColor(color.x, color.y, color.z);
            handle.markDirty();
        }
    }

    /** Convenience overload that colours the light with the plume body colour (ctx.mid()). */
    public static void update(int key, Vec3 pos, PlumeContext ctx, float radius, float brightness, boolean active) {
        Vector3f c = PlumeContext.rgb(ctx.mid());
        update(key, pos, c, radius, brightness, active);
    }

    public static void stopAll() {
        for (var handle : ACTIVE.values()) {
            if (handle != null) {
                handle.free();
            }
        }
        ACTIVE.clear();
    }

    /** Frees lights whose compartment stopped rendering for more than a second. */
    public static void sweep() {
        long now = System.nanoTime();
        var it = ACTIVE.int2ObjectEntrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            int key = e.getIntKey();
            long last = LAST_SEEN.get(key);
            if (last < 0 || now - last > 1_000_000_000L) { // not seen for >1s
                e.getValue().free();
                it.remove();
                LAST_SEEN.remove(key);
            }
        }
    }
}
