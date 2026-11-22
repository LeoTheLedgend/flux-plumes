package dev.leo.fluxplumes;

import mekanism.api.text.IHasTextComponent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/** The geometry/shader family used by a player-authored CUSTOM plume. */
public enum PlumeShape implements IHasTextComponent {
    FLAME,
    DIAMONDS;

    @Override
    public @NotNull Component getTextComponent() {
        return Component.translatable("gui.fluxplumes.plume_shape." + name().toLowerCase(java.util.Locale.ROOT));
    }
}
