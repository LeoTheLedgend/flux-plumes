package dev.leo.fluxplumes.net;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class FPNetwork {
    private FPNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playBidirectional(SetPlumePayload.TYPE, SetPlumePayload.STREAM_CODEC, (payload, ctx) ->
                ctx.enqueueWork(() -> {
                    // TODO server side: validate the player can edit the controller at payload.controller(),
                    //   store payload.settings() in its FPAttachments.PLUME attachment, then re-send to trackers.
                    // TODO client side: cache payload.settings() by controller position for the renderer.
                }));
    }
}
