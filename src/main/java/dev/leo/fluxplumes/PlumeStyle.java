package dev.leo.fluxplumes;

import mekanism.api.text.IHasTextComponent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.Locale;

/** Available exhaust profiles; DEFAULT retains Fluxnautics' original effect. */
public enum PlumeStyle implements IHasTextComponent {
    DEFAULT, ROCKET, SR71, FLARE;

    @Override
    public @NotNull Component getTextComponent() {
        return Component.translatable("gui.fluxplumes.plume_style." + name().toLowerCase(Locale.ROOT));
    }
}
