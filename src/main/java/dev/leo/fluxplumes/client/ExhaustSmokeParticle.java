package dev.leo.fluxplumes.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A big soft billowing smoke puff. Starts hot (glowing, tinted by the plume's body colour) and cools to the
 * smoke colour while it expands and fades.
 */
public class ExhaustSmokeParticle extends TextureSheetParticle {
    private static final float BASE_ALPHA = 0.55f;

    /** Set when the client registers our particle sprites. */
    @Nullable
    public static SpriteSet sprites;

    private final float startSize;
    private final float endSize;
    private final float heat0;
    private final float opacity;
    private final float spin;
    private final float sr, sg, sb; // smoke colour
    private final float hr, hg, hb; // hot colour
    private float heat;

    public ExhaustSmokeParticle(ClientLevel level, double x, double y, double z,
                                double vx, double vy, double vz,
                                int smokeRgb, int hotRgb, float heat0,
                                float halfSize, float growth, boolean physics, float opacity, float lifetimeScale) {
        super(level, x, y, z);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.lifetime = (int) ((40 + this.random.nextInt(30)) * lifetimeScale);
        this.lifetime = Math.max(20, Math.min(this.lifetime, 120));
        this.hasPhysics = physics;
        this.gravity = -0.003f; // negative = drifts upward
        this.friction = 0.95f;

        this.startSize = halfSize;
        this.endSize = halfSize * growth;
        this.quadSize = halfSize;
        this.heat0 = heat0;
        this.opacity = opacity;
        this.heat = heat0;
        this.roll = this.random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;
        this.spin = (this.random.nextFloat() - 0.5f) * 0.04f;

        this.sr = ((smokeRgb >> 16) & 0xFF) / 255f;
        this.sg = ((smokeRgb >> 8) & 0xFF) / 255f;
        this.sb = (smokeRgb & 0xFF) / 255f;
        this.hr = ((hotRgb >> 16) & 0xFF) / 255f;
        this.hg = ((hotRgb >> 8) & 0xFF) / 255f;
        this.hb = (hotRgb & 0xFF) / 255f;

        this.alpha = 0f;
        this.rCol = Mth.lerp(heat0, sr, hr);
        this.gCol = Mth.lerp(heat0, sg, hg);
        this.bCol = Mth.lerp(heat0, sb, hb);

        if (sprites != null) {
            this.setSprite(sprites.get(this.random.nextInt(4), 4)); // sprites 0-3 are smoke, 4 is the trail sprite
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
        this.oRoll = this.roll;
        this.roll += this.spin;
        // small random push so the cloud billows instead of drifting in straight lines
        float push = (this.random.nextFloat() - 0.5f) * 0.006f;
        this.xd += push;
        this.yd += push;
        this.zd += push;

        this.quadSize = Mth.lerp(Mth.sqrt(life), startSize, endSize); // expands fast, then slows
        float fadeIn = Mth.clamp(life / 0.12f, 0f, 1f);
        float decay = 1f - life;
        this.alpha = BASE_ALPHA * opacity * fadeIn * decay * decay;

        this.heat = heat0 * decay * decay;
        this.rCol = Mth.lerp(heat, sr, hr);
        this.gCol = Mth.lerp(heat, sg, hg);
        this.bCol = Mth.lerp(heat, sb, hb);
    }

    /** Hot smoke is lit by the flame, so it glows regardless of the surrounding light. */
    @Override
    public int getLightColor(float partialTick) {
        int base = super.getLightColor(partialTick);
        int block = Math.max(LightTexture.block(base), (int) (15f * heat));
        return LightTexture.pack(block, LightTexture.sky(base));
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
