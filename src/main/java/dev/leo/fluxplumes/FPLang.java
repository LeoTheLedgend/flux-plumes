package dev.leo.fluxplumes;

import mekanism.api.text.IHasTranslationKey;

/** Translation keys for the labels of our group settings (Fluxnautics' GUI reads these through IHasTranslationKey). */
public enum FPLang implements IHasTranslationKey {
    STYLE("gui.fluxplumes.group.plume_style"),
    CUSTOM_SHAPE("gui.fluxplumes.group.plume_custom_shape"),
    MOTION("gui.fluxplumes.group.plume_motion"),
    SCALE("gui.fluxplumes.group.plume_scale"),
    MODE("gui.fluxplumes.group.plume_color_mode"),
    OPACITY("gui.fluxplumes.group.plume_opacity"),
    SHAKE("gui.fluxplumes.group.plume_shake"),
    TRAIL("gui.fluxplumes.group.plume_trail"),
    BASE("gui.fluxplumes.group.plume_color"),
    CORE("gui.fluxplumes.group.plume_core"),
    MID("gui.fluxplumes.group.plume_mid"),
    OUTER("gui.fluxplumes.group.plume_outer"),
    ACCENT("gui.fluxplumes.group.plume_accent");

    private final String key;

    FPLang(String key) {
        this.key = key;
    }

    @Override
    public String getTranslationKey() {
        return key;
    }
}
