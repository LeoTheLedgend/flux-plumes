package dev.leo.fluxplumes.client;

import dev.leo.fluxplumes.FluxPlumes;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.forge.event.ForgeVeilPostProcessingEvent;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.Arrays;

/**
 * Feeds the heat haze post pipeline: projects each recorded plume from world space to screen
 * space using Veil's current camera matrices and uploads them as uniforms right before the
 * pass runs. Fires on the NeoForge game bus each frame (ForgeVeilPostProcessingEvent).
 */
@EventBusSubscriber(modid = FluxPlumes.MODID, value = Dist.CLIENT)
public final class FPHazeEvents {
    private static final float[] DATA = new float[32];
    private static final Vector4f V = new Vector4f();
    private static final Matrix4f MVP = new Matrix4f();

    private FPHazeEvents() {}

    @SubscribeEvent
    public static void prePost(ForgeVeilPostProcessingEvent.Pre event) {
        if (!event.getName().equals(PlumeHaze.PIPELINE)) {
            return;
        }
        int n = PlumeHaze.count();
        if (n <= 0) {
            PlumeHaze.clear();
            return;
        }

        var matrices = VeilRenderSystem.renderer().getCameraMatrices();
        MVP.set(matrices.getProjectionMatrix()).mul(matrices.getViewMatrix());

        float w = Minecraft.getInstance().getWindow().getScreenWidth();
        float h = Minecraft.getInstance().getWindow().getScreenHeight();
        // NDC.y = proj.m11 * yView / wClip, so screen px per world unit at depth = |proj.m11| * h / (2|w|).
        float projY = Math.abs(MVP.m11());

        int idx = 0;
        for (int i = 0; i < n && idx < 32; i++) {
            double[] p = PlumeHaze.get(i);
            if (p == null) {
                continue;
            }
            V.set((float) p[0], (float) p[1], (float) p[2], 1f).mul(MVP);
            if (V.w <= 0.0001f) {
                continue; // behind the camera
            }
            float sx = (V.x / V.w + 1f) * 0.5f;
            float sy = (1f - V.y / V.w) * 0.5f;
            if (sx < -0.5f || sx > 1.5f || sy < -0.5f || sy > 1.5f) {
                continue; // off screen
            }
            float screenRadiusPx = (float) p[3] * projY * (0.5f * h) / V.w;
            DATA[idx++] = sx;
            DATA[idx++] = sy;
            DATA[idx++] = Math.max(screenRadiusPx / w, 0.004f);
            DATA[idx++] = (float) p[4];
        }

        if (idx == 0) {
            PlumeHaze.clear();
            return;
        }
        var pipeline = event.getPipeline();
        if (pipeline == null) {
            return;
        }
        pipeline.getUniformSafe("uHazeCount").setInt(idx / 4);
        if (idx > 0) {
            pipeline.getUniformSafe("uHazeData").setFloats(Arrays.copyOf(DATA, idx));
        }
        pipeline.getUniformSafe("uHazeTime").setFloat((float) (System.nanoTime() / 1.0e9));
    }
}
