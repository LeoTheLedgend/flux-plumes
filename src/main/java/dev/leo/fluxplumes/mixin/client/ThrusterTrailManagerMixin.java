package dev.leo.fluxplumes.mixin.client;

import dev.alyti.fluxnautics.client.render.ThrusterTrailManager;
import dev.leo.fluxplumes.client.PlumeContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fluxnautics records its motion trail from inside renderWalls, while our PlumeContext is set.
 * For our styles we skip it, and PlumeTrail draws our own trail instead.
 */
@Mixin(ThrusterTrailManager.class)
public abstract class ThrusterTrailManagerMixin {

    @Inject(method = "recordFromRenderer", at = @At("HEAD"), cancellable = true)
    private static void fluxplumes$skipStockTrail(CallbackInfo ci) {
        if (PlumeContext.current != null) {
            ci.cancel();
        }
    }
}
