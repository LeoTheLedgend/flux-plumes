package dev.leo.fluxplumes.client;

import dev.leo.fluxplumes.FluxPlumes;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Reclaim effects after their thruster stops rendering. */
@EventBusSubscriber(modid = FluxPlumes.MODID, value = Dist.CLIENT)
public final class FPClientTicker {
    private FPClientTicker() {}

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        var level = Minecraft.getInstance().level;
        if (level != null && level.getGameTime() % 20 == 0) {
            PlumeLight.sweep();
            PlumeSound.sweep();
        }
    }
}
