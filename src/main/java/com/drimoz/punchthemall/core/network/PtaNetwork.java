package com.drimoz.punchthemall.core.network;

import com.drimoz.punchthemall.PunchThemAll;
import com.drimoz.punchthemall.core.codec.InteractionSpec;
import com.drimoz.punchthemall.core.registry.InteractionRegistry;
import com.drimoz.punchthemall.core.event.PlayerInteractionHandler;
import com.drimoz.punchthemall.core.util.PTALoggers;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Registers PTA's payloads: interactions out to clients, and the left-click-on-nothing signal
 * back in.
 *
 * <p>The registrar is {@code optional()}. Every gameplay decision is made server-side, so a client
 * without PunchThemAll can play on a PunchThemAll server perfectly well — it simply has no JEI/EMI
 * entries for the pack's interactions. A required registrar would refuse those clients at handshake
 * for the sake of a display feature.</p>
 *
 * <p>{@code PROTOCOL_VERSION} must be bumped whenever a payload's shape changes, or a mismatched
 * client and server will believe they agree and fail at decode time instead.</p>
 */
@EventBusSubscriber(modid = PunchThemAll.MOD_ID)
public class PtaNetwork {

    private static final String PROTOCOL_VERSION = "2";

    /** Batches accumulated since the last {@code first} payload, awaiting the {@code last} one. */
    private static final Map<Identifier, InteractionSpec> PENDING = new HashMap<>();

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(PROTOCOL_VERSION)
                .optional()
                .playToClient(
                        SyncInteractionsPayload.TYPE,
                        SyncInteractionsPayload.STREAM_CODEC,
                        PtaNetwork::handleOnClient
                )
                .playToServer(
                        LeftClickEmptyPayload.TYPE,
                        LeftClickEmptyPayload.STREAM_CODEC,
                        PtaNetwork::handleLeftClickEmpty
                );
    }

    private static void handleLeftClickEmpty(LeftClickEmptyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PlayerInteractionHandler.onLeftClickEmptyFromClient(context.player()));
    }

    private static void handleOnClient(SyncInteractionsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (payload.first()) {
                PENDING.clear();
            }
            PENDING.putAll(payload.specs());

            // Rebuild only once the whole series has arrived, so the registry never holds a partial
            // set and the viewers are not refreshed once per batch.
            if (!payload.last()) return;

            Map<Identifier, InteractionSpec> received = Map.copyOf(PENDING);
            PENDING.clear();

            InteractionRegistry.getInstance().rebuildFrom(received, context.player().registryAccess());
            refreshViewers();
        });
    }

    /** Drop anything half-received. Called when the client leaves a server mid-sync. */
    public static void clearPending() {
        PENDING.clear();
    }

    /** Push the current registry into whichever recipe viewers are installed. */
    public static void refreshViewers() {
        // Guarded so the plugin classes are only loaded when the viewer is actually present.
        if (ModList.get().isLoaded("jei")) {
            try {
                com.drimoz.punchthemall.jei.JEIPlugin.refreshFromRegistry();
            } catch (RuntimeException e) {
                PTALoggers.error("Could not refresh the JEI category: " + e);
            }
        }
    }
}
