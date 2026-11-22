package dev.leo.fluxplumes.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

/** One soft glowing dot of the custom trail. It stays where it was spawned, so the trail is rock steady. */
public class TrailParticle extends TextureSheetParticle {
    private final float startSize;
    private final float endSize;
    private final float baseAlpha;
    private final float sr, sg, sb;
    private final float er, eg, eb;

    public TrailParticle(ClientLevel level, double x, double y, double z,
                         int startRgb, int endRgb, float halfSize, float endScale, int lifetime, float baseAlpha) {
        super(level, x, y, z);
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        this.gravity = 0f;
        this.hasPhysics = false;
        this.lifetime = lifetime;
        this.startSize = halfSize;
        this.endSize = halfSize * endScale;
        this.quadSize = halfSize;
        this.baseAlpha = baseAlpha;
        this.sr = ((startRgb >> 16) & 0xFF) / 255f;
        this.sg = ((startRgb >> 8) & 0xFF) / 255f;
        this.sb = (startRgb & 0xFF) / 255f;
        this.er = ((endRgb >> 16) & 0xFF) / 255f;
        this.eg = ((endRgb >> 8) & 0xFF) / 255f;
        this.eb = (endRgb & 0xFF) / 255f;
        this.rCol = sr;
        this.gCol = sg;
        this.bCol = sb;
        this.alpha = baseAlpha;
        if (ExhaustSmokeParticle.sprites != null) {
            this.setSprite(ExhaustSmokeParticle.sprites.get(4, 4)); // the trail sprite
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
        this.quadSize = Mth.lerp(life, startSize, endSize);
        this.alpha = baseAlpha * (float) Math.pow(1f - life, 1.5);
        this.rCol = Mth.lerp(life, sr, er);
        this.gCol = Mth.lerp(life, sg, eg);
        this.bCol = Mth.lerp(life, sb, eb);
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0; // always full bright, it is glowing
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
