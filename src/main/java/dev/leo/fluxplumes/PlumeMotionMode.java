package dev.leo.fluxplumes;

import mekanism.api.text.IHasTextComponent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/** Controls how much the plume bends and flickers while thrust is applied. */
public enum PlumeMotionMode implements IHasTextComponent {
    STEADY(0.25f),
    FLOWING(0.65f),
    WILD(1.25f);

    private final float multiplier;

    PlumeMotionMode(float multiplier) { this.multiplier = multiplier; }
    public float multiplier() { return multiplier; }

    @Override public @NotNull Component getTextComponent() {
        return Component.translatable("gui.fluxplumes.plume_motion." + name().toLowerCase(java.util.Locale.ROOT));
    }
}
