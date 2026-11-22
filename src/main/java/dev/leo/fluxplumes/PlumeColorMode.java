package dev.leo.fluxplumes;

import mekanism.api.text.IHasTextComponent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/** SIMPLE: one colour, the rest of the palette is derived. ADVANCED: use the four individual colours. */
public enum PlumeColorMode implements IHasTextComponent {
    SIMPLE,
    ADVANCED;

    @Override
    public @NotNull Component getTextComponent() {
        return Component.translatable("gui.fluxplumes.plume_color_mode." + name().toLowerCase(Locale.ROOT));
    }
}
