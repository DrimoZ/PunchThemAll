package com.drimoz.punchthemall.core.network;

import com.drimoz.punchthemall.PunchThemAll;
import com.drimoz.punchthemall.core.registry.InteractionRegistry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Registers PTA's single payload. Bumping {@code PROTOCOL_VERSION} makes clients on an older
 * protocol fail the handshake rather than silently mis-parse the spec stream.
 */
@EventBusSubscriber(modid = PunchThemAll.MOD_ID)
public class PtaNetwork {

    private static final String PROTOCOL_VERSION = "1";

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(PROTOCOL_VERSION).playToClient(
                SyncInteractionsPayload.TYPE,
                SyncInteractionsPayload.STREAM_CODEC,
                PtaNetwork::handleOnClient
        );
    }

    private static void handleOnClient(SyncInteractionsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            InteractionRegistry.getInstance().rebuildFrom(payload.specs(), context.player().registryAccess());

            // Push the new set into the viewers (guarded so the classes only load when present).
            if (ModList.get().isLoaded("jei")) {
                com.drimoz.punchthemall.jei.JEIPlugin.refreshFromRegistry();
            }
        });
    }
}
