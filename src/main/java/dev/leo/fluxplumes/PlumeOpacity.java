package dev.leo.fluxplumes;

import mekanism.api.text.IHasTextComponent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/** Plume transparency in steps. Shown as a slider by Fluxnautics' enum widget. */
public enum PlumeOpacity implements IHasTextComponent {
    P20(0.2f),
    P40(0.4f),
    P60(0.6f),
    P80(0.8f),
    P100(1.0f);

    private final float value;

    PlumeOpacity(float value) {
        this.value = value;
    }

    public float value() {
        return value;
    }

    @Override
    public @NotNull Component getTextComponent() {
        return Component.translatable("gui.fluxplumes.plume_opacity." + name().toLowerCase(Locale.ROOT));
    }
}
