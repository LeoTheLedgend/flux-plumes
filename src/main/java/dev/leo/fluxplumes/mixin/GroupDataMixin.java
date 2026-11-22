package dev.leo.fluxplumes.mixin;

import dev.alyti.fluxnautics.config.group.GroupConfig;
import dev.alyti.fluxnautics.multiblock.ThrusterArrayMultiblockData;
import dev.leo.fluxplumes.FPConfigs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fluxnautics saves, syncs and edits group settings generically from GroupData's config list, and its
 * controller GUI builds a widget per config. Adding ours here gets all of that for free.
 */
@Mixin(ThrusterArrayMultiblockData.GroupData.class)
public abstract class GroupDataMixin {

    @Shadow
    abstract void addConfig(GroupConfig<?, ?> config);

    @Inject(method = "<init>", at = @At("TAIL"))
    private void fluxplumes$addConfigs(CallbackInfo ci) {
        FPConfigs.addAll(this::addConfig);
    }
}
