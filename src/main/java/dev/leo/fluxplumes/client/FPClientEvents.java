package dev.leo.fluxplumes.client;

import dev.leo.fluxplumes.FPParticles;
import dev.leo.fluxplumes.FluxPlumes;
import net.minecraft.client.particle.Particle;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = FluxPlumes.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FPClientEvents {
    private FPClientEvents() {}

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(FPParticles.EXHAUST_SMOKE.get(), sprites -> {
            ExhaustSmokeParticle.sprites = sprites; // our spawner creates particles directly and needs these
            return (type, level, x, y, z, vx, vy, vz) -> {
                Particle p = new ExhaustSmokeParticle(level, x, y, z, vx, vy, vz,
                    0xFFFFFF, 0xFFB347, 0f, 1f, 2f, false, 1f, 1f);
                return p;
            };
        });
        event.registerSpriteSet(FPParticles.HEAT_SHIMMER.get(), sprites -> {
            HeatShimmerParticle.sprites = sprites;
            return (type, level, x, y, z, vx, vy, vz) -> {
                Particle p = new HeatShimmerParticle(level, x, y, z, 0.25f, 3f, 0f, 0f);
                return p;
            };
        });
    }
}
