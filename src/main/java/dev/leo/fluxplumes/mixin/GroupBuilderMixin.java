package dev.leo.fluxplumes.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.alyti.fluxnautics.multiblock.ThrusterArrayMultiblockData;
import dev.leo.fluxplumes.FPConfigs;
import mekanism.common.integration.computer.ComputerException;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ComputerCraft: ensureGroupsExistDetailed rejects unknown keys, so pull ours out first,
 * let Fluxnautics parse the rest, then merge ours into the resulting config tag.
 */
@Mixin(ThrusterArrayMultiblockData.GroupBuilder.class)
public abstract class GroupBuilderMixin {

    @WrapMethod(method = "of")
    private static ThrusterArrayMultiblockData.GroupBuilder fluxplumes$of(
            Map<?, ?> value, Operation<ThrusterArrayMultiblockData.GroupBuilder> original) throws ComputerException {
        Map<Object, Object> theirs = new LinkedHashMap<>();
        CompoundTag ours = new CompoundTag();
        for (Map.Entry<?, ?> entry : value.entrySet()) {
            if (entry.getKey() instanceof String key && FPConfigs.isOurs(key)) {
                FPConfigs.parseInto(ours, key, entry.getValue());
            } else {
                theirs.put(entry.getKey(), entry.getValue());
            }
        }
        ThrusterArrayMultiblockData.GroupBuilder builder = original.call(theirs);
        builder.configs.merge(ours);
        return builder;
    }
}
