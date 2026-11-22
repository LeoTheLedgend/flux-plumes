package dev.leo.fluxplumes;

import dev.leo.fluxplumes.effect.PlumeSettings;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class FPAttachments {
    public static final DeferredRegister<AttachmentType<?>> REGISTER =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, FluxPlumes.MODID);

    /**
     * Plume settings stored on a thruster array controller block entity (saved to disk).
     * Block entity attachments are not auto-synced to clients in 1.21.1, so FPNetwork pushes updates.
     * v0 is one setting per controller; Fluxnautics keeps effects per thruster GROUP, so this becomes
     * a groupId -> PlumeSettings map once we've read how groups are keyed.
     */
    public static final Supplier<AttachmentType<PlumeSettings>> PLUME = REGISTER.register("plume",
            () -> AttachmentType.builder(() -> PlumeSettings.DEFAULT)
                    .serialize(PlumeSettings.MAP_CODEC.codec())
                    .build());

    private FPAttachments() {}
}
