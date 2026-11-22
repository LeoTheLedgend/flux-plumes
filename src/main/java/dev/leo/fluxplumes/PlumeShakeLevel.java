package dev.leo.fluxplumes;

import mekanism.api.text.IHasTextComponent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/** How much the camera shakes near a plume. */
public enum PlumeShakeLevel implements IHasTextComponent {
    OFF(0f),
    LOW(0.5f),
    MEDIUM(1.0f),
    HIGH(1.8f);

    private final float multiplier;

    PlumeShakeLevel(float multiplier) {
        this.multiplier = multiplier;
    }

    public float multiplier() {
        return multiplier;
    }

    @Override
    public @NotNull Component getTextComponent() {
        return Component.translatable("gui.fluxplumes.plume_shake." + name().toLowerCase(Locale.ROOT));
    }
}
