package dev.leo.fluxplumes;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class FPParticles {
    public static final DeferredRegister<ParticleType<?>> REGISTER =
            DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, FluxPlumes.MODID);

    public static final Supplier<SimpleParticleType> EXHAUST_SMOKE =
            REGISTER.register("exhaust_smoke", () -> new SimpleParticleType(false));

    public static final Supplier<SimpleParticleType> HEAT_SHIMMER =
            REGISTER.register("heat_shimmer", () -> new SimpleParticleType(false));

    private FPParticles() {}
}
