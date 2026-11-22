package dev.leo.fluxplumes;

import mekanism.api.text.IHasTextComponent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/** Whether our own trail is drawn behind the thruster (it replaces Fluxnautics' trail for our styles). */
public enum PlumeTrailMode implements IHasTextComponent {
    OFF,
    ON;

    @Override
    public @NotNull Component getTextComponent() {
        return Component.translatable("gui.fluxplumes.plume_trail." + name().toLowerCase(Locale.ROOT));
    }
}
