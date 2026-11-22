package dev.leo.fluxplumes.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.alyti.fluxnautics.client.render.ThrusterCompartmentCache;
import dev.alyti.fluxnautics.content.blocks.thruster.IonThrusterRenderer;
import dev.alyti.fluxnautics.content.blocks.thruster.ThrusterArrayControllerBlockEntity;
import dev.leo.fluxplumes.PlumeStyle;
import dev.leo.fluxplumes.client.GlUniforms;
import dev.leo.fluxplumes.client.PlumeContext;
import dev.leo.fluxplumes.client.PlumeHeat;
import dev.leo.fluxplumes.client.PlumeLight;
import dev.leo.fluxplumes.client.PlumeMotion;
import dev.leo.fluxplumes.client.PlumeShake;
import dev.leo.fluxplumes.client.PlumeSmoke;
import dev.leo.fluxplumes.client.PlumeSound;
import dev.leo.fluxplumes.client.PlumeTrail;
import dev.leo.fluxplumes.effect.PlumeMath;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Objects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IonThrusterRenderer.class)
public abstract class IonThrusterRendererMixin {

    /** Per-compartment effects, then swap in our render type (and so our shader) for our styles. */
    @WrapMethod(method = "renderWalls")
    private void fluxplumes$renderWalls(ThrusterArrayControllerBlockEntity controller,
                                        ThrusterCompartmentCache.Compartment compartment,
                                        PoseStack pose, MultiBufferSource buffer,
                                        float length, float ticks, Vector4f color,
                                        float shiftX, float shiftY, float span, float fade, float seed,
                                        @Nullable RenderType renderType,
                                        Operation<Void> original) {
        PlumeContext ctx = PlumeContext.resolve(controller, compartment, color);
        if (ctx != null) {
            Matrix4f matrix = pose.last().pose();
            float heat = PlumeHeat.update(compartment, length, span);
            // Each plume family has its own inertia: ion exhaust is nearly massless while the
            // aurora field keeps a broad, lingering wake.
            float motionStrength = switch (ctx.style()) {
                case SR71 -> 0.35f;
                default -> 0.8f;
            };
            PlumeMotion.update(compartment, matrix, length,
                    motionStrength * ctx.motion().multiplier());
            PlumeShake.record(compartment, matrix, length, span, ctx);
            PlumeTrail.emit(compartment, matrix, length, span, ctx, heat);
            // Aurora and ion exhaust are clean plasma fields. Only combustion-style plumes emit smoke.
            if (ctx.style() == PlumeStyle.ROCKET) {
                PlumeSmoke.emit(controller, compartment, matrix, length, span, ctx);
            } else if (ctx.style() == PlumeStyle.SR71 && heat < 0.75f) {
                // A freshly lit afterburner burns like a rocket first: same smoke, fading as it heats up.
                float amount = 1f - heat / 0.75f;
                int smoke = PlumeMath.lerpColor(0xEFE6D8, ctx.accent(), heat);
                int hot = PlumeMath.lerpColor(0xFFA040, ctx.mid(), heat);
                PlumeSmoke.emit(controller, compartment, matrix, length, span, ctx, amount, smoke, hot);
            }
            // Looping thruster sound per style, positioned at the nozzle.
            float thrust = length / Math.max(span, 1f);
            boolean firing = thrust > 0.12f;
            int key = Objects.hash(compartment.anchor.pos, compartment.groupId);
            SoundEvent event = PlumeSound.forStyle(ctx.style());
            if (event != null) {
                Vector3f nozzle = new Vector3f(0f, 0f, 0f).mulPosition(matrix);
                Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
                PlumeSound.update(key, event, cam.x + nozzle.x, cam.y + nozzle.y, cam.z + nozzle.z, firing);
            }
            // Colored dynamic light at the nozzle, scaled by plume size and thruster count.
            Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            Vector3f nozzle = new Vector3f(0f, 0f, 0f).mulPosition(matrix);
            float[] bounds = PlumeMotion.bounds(compartment);
            float plumeRadius = bounds != null ? Math.max(bounds[2], 0.4f) : 0.5f;
            float sizeScale = 1f + 0.5f * Math.min(plumeRadius, 3f);
            float countScale = 1f + 0.15f * Math.max(0, compartment.members.size() - 1);
            float brightness = 1.2f * countScale * (0.6f + 0.4f * Math.min(thrust, 1.5f));
            float radius = 6f * sizeScale;
            PlumeLight.update(key, new Vec3(cam.x + nozzle.x, cam.y + nozzle.y, cam.z + nozzle.z),
                    PlumeContext.rgb(ctx.mid()), radius, brightness, firing);
        } else {
            // No plume style: use Fluxnautics' own plume colour for the light, scaled by size and thruster count.
            int key = Objects.hash(compartment.anchor.pos, compartment.groupId);
            float thrust = length / Math.max(span, 1f);
            boolean firing = thrust > 0.12f;
            // Sound for the default plume too
            SoundEvent event = PlumeSound.forStyle(PlumeStyle.DEFAULT);
            if (event != null) {
                Matrix4f matrix = pose.last().pose();
                Vector3f nozzle = new Vector3f(0f, 0f, 0f).mulPosition(matrix);
                Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
                PlumeSound.update(key, event, cam.x + nozzle.x, cam.y + nozzle.y, cam.z + nozzle.z, firing);
            }
            float[] bounds = PlumeMotion.bounds(compartment);
            float plumeRadius = bounds != null ? Math.max(bounds[2], 0.4f) : 0.5f;
            float sizeScale = 1f + 0.5f * Math.min(plumeRadius, 3f);
            float countScale = 1f + 0.15f * Math.max(0, compartment.members.size() - 1);
            float brightness = 1.2f * countScale * (0.6f + 0.4f * Math.min(thrust, 1.5f));
            float radius = 6f * sizeScale;
            if (firing) {
                Matrix4f matrix = pose.last().pose();
                Vector3f nozzle = new Vector3f(0f, 0f, 0f).mulPosition(matrix);
                Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
                PlumeLight.update(key, new Vec3(cam.x + nozzle.x, cam.y + nozzle.y, cam.z + nozzle.z),
                        new Vector3f(color.x(), color.y(), color.z()), radius, brightness, true);
            } else {
                PlumeLight.update(key, null, (Vector3f) null, 0f, 0f, false);
            }

        }
        RenderType swapped = ctx == null ? null : ctx.renderType();
        float visualLength = ctx == null ? length : length * ctx.scale().multiplier();
        PlumeContext.current = swapped == null ? null : ctx;
        try {
            original.call(controller, compartment, pose, buffer, visualLength, ticks, color,
                    shiftX, shiftY, span, fade, seed, swapped != null ? swapped : renderType);
            if (swapped != null && ctx.style() == PlumeStyle.FLARE) {
                float[] bounds = PlumeMotion.bounds(compartment);
                if (bounds != null) {
                    pose.pushPose();
                    try {
                        // Scale around the actual nozzle centre, not the world origin.
                        pose.translate(bounds[0], bounds[1], 0);
                        pose.scale(0.58f, 0.58f, 1f);
                        pose.translate(-bounds[0], -bounds[1], 0);
                        PlumeContext.current = new PlumeContext(ctx.style(), ctx.shape(), ctx.motion(), ctx.scale(),
                                ctx.core(), ctx.core(), ctx.mid(), ctx.accent(), ctx.ground(), ctx.opacity(), ctx.shake(), ctx.trail());
                        original.call(controller, compartment, pose, buffer, visualLength * 0.72f,
                                ticks, color, shiftX, shiftY, span, fade, seed, swapped);
                    } finally {
                        pose.popPose();
                    }
                }
            }

        } finally {
            PlumeContext.current = null;
        }
    }

    /** Fluxnautics has just set its own uniforms; add ours right before the batch is drawn. */
    @Inject(method = "renderWalls",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch(Lnet/minecraft/client/renderer/RenderType;)V"))
    private void fluxplumes$setUniforms(CallbackInfo ci) {
        PlumeContext ctx = PlumeContext.current;
        if (ctx == null) {
            return;
        }
        ShaderProgram shader = VeilRenderSystem.getShader();
        if (shader == null) {
            return;
        }
        int program = shader.getProgram();

        Vector3f core = PlumeContext.rgb(ctx.core());
        Vector3f mid = PlumeContext.rgb(ctx.mid());
        Vector3f outer = PlumeContext.rgb(ctx.outer());
        Vector3f accent = PlumeContext.rgb(ctx.accent());

        GlUniforms.set3f(program, "uFPCore", core.x(), core.y(), core.z());
        GlUniforms.set3f(program, "uFPMid", mid.x(), mid.y(), mid.z());
        GlUniforms.set3f(program, "uFPOuter", outer.x(), outer.y(), outer.z());
        GlUniforms.set3f(program, "uFPAccent", accent.x(), accent.y(), accent.z());
        GlUniforms.set1f(program, "uFPGround", ctx.ground());
        GlUniforms.set1f(program, "uFPOpacity", ctx.opacity());
        // Ion geometry is always a pinched beam; it must not spend its first few seconds using the
        // SR-71 profile's wide cold-start flare.
        GlUniforms.set1f(program, "uFPHeat", PlumeHeat.current);
        GlUniforms.set3fv(program, "uFPBend[0]", PlumeMotion.curve);
    }
}
