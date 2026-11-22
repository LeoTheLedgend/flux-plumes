package dev.leo.fluxplumes;

import mekanism.api.text.IHasTextComponent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/** Controls the rendered plume length without changing engine performance. */
public enum PlumeScale implements IHasTextComponent {
    COMPACT(0.7f),
    STANDARD(1.0f),
    EXTENDED(1.45f);

    private final float multiplier;
    PlumeScale(float multiplier) { this.multiplier = multiplier; }
    public float multiplier() { return multiplier; }

    @Override public @NotNull Component getTextComponent() {
        return Component.translatable("gui.fluxplumes.plume_scale." + name().toLowerCase(java.util.Locale.ROOT));
    }
}
