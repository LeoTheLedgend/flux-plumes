package dev.leo.fluxplumes.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A short-lived thermal mirage / heat shimmer particle. It drifts upward from a hot rocket plume,
 * pulsing in alpha so the air looks like it's shimmering. Uses very few particles and no physics
 * so it stays cheap even with several compartments firing.
 */
public class HeatShimmerParticle extends TextureSheetParticle {
    @Nullable
    public static SpriteSet sprites;

    private final float baseAlpha;
    private final float pulseSpeed;
    private final float driftX;
    private final float driftZ;
    private final float life0;

    public HeatShimmerParticle(ClientLevel level, double x, double y, double z,
                               float baseAlpha, float pulseSpeed, float driftX, float driftZ) {
        super(level, x, y, z);
        this.xd = 0;
        this.yd = 0.02; // gentle upward drift
        this.zd = 0;
        this.gravity = 0f;
        this.hasPhysics = false;
        this.lifetime = 20 + this.random.nextInt(20);
        this.quadSize = 0.6f + this.random.nextFloat() * 0.6f;
        this.baseAlpha = baseAlpha;
        this.pulseSpeed = pulseSpeed;
        this.driftX = driftX;
        this.driftZ = driftZ;
        this.life0 = this.random.nextFloat() * Mth.TWO_PI; // random phase so particles don't pulse in sync
        this.alpha = 0f;
        this.rCol = 1f;
        this.gCol = 1f;
        this.bCol = 1f;

        if (sprites != null) {
            this.setSprite(sprites.get(this.random.nextInt(4), 4));
        } else {
            this.remove();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) {
            return;
        }
        float life = (float) this.age / (float) this.lifetime;
        // Drift sideways slightly so the shimmer isn't a straight vertical line.
        this.x += driftX * 0.05f;
        this.z += driftZ * 0.05f;
        // Pulse alpha: bright in the middle of life, fades at start and end.
        float pulse = 0.5f + 0.5f * Mth.sin(life * Mth.TWO_PI * pulseSpeed + life0);
        this.alpha = baseAlpha * pulse * (1f - life);
        // Grow slightly as it rises, like real heat haze expanding.
        this.quadSize = 0.6f + life * 0.8f;
        this.roll += 0.02f;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0; // full bright, it is heat haze
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
