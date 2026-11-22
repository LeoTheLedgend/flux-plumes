package dev.leo.fluxplumes.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Client-only registration kept out of the dedicated-server class loading path. */
@OnlyIn(Dist.CLIENT)
public final class ClientBootstrap {
    private ClientBootstrap() {
    }

    public static void register(IEventBus modBus) {
        PlumeSound.REGISTER.register(modBus);
    }
}
