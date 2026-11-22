package dev.leo.fluxplumes;

import com.mojang.logging.LogUtils;
import dev.leo.fluxplumes.client.ClientBootstrap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Flux Plumes is mostly mixin driven: GroupDataMixin adds our settings to every thruster group,
 * GroupBuilderMixin exposes them to ComputerCraft, and IonThrusterRendererMixin swaps in our shaders.
 */
@Mod(FluxPlumes.MODID)
public class FluxPlumes {
    public static final String MODID = "fluxplumes";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FluxPlumes(IEventBus modBus) {
        FPParticles.REGISTER.register(modBus);
        // Sound instances and their Minecraft client dependencies must never be linked by a
        // dedicated server. Keeping this boundary explicit makes Flux Plumes safe in modpacks.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientBootstrap.register(modBus);
        }
        LOGGER.info("Flux Plumes loaded");
    }
}
