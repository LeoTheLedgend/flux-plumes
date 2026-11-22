package dev.leo.fluxplumes.net;

import dev.leo.fluxplumes.FluxPlumes;
import dev.leo.fluxplumes.effect.PlumeSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client -> server: the GUI changed the settings. Server -> client: authoritative settings for rendering. */
public record SetPlumePayload(BlockPos controller, PlumeSettings settings) implements CustomPacketPayload {
    public static final Type<SetPlumePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(FluxPlumes.MODID, "set_plume"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetPlumePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SetPlumePayload::controller,
            PlumeSettings.STREAM_CODEC, SetPlumePayload::settings,
            SetPlumePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
