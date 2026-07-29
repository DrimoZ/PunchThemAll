package com.drimoz.punchthemall.core.event;

import com.drimoz.punchthemall.PunchThemAll;
import com.drimoz.punchthemall.core.network.SyncInteractionsPayload;
import com.drimoz.punchthemall.core.registry.InteractionRegistry;
import com.drimoz.punchthemall.core.registry.InteractionReloadListener;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Server-side loading and syncing of interactions.
 *
 * <p>{@link AddServerReloadListenersEvent} fires both at server start and on {@code /reload}, so one
 * listener covers both. {@link OnDatapackSyncEvent} then fires for the joining player, or for every
 * player after a reload — {@code getRelevantPlayers()} already encodes that distinction, so a single
 * handler serves both cases.</p>
 */
@EventBusSubscriber(modid = PunchThemAll.MOD_ID)
public class PtaServerEvents {

    /**
     * Listeners are named now, and the listener no longer takes the registries and the condition
     * context by constructor: NeoForge injects both, so {@code InteractionReloadListener} reads them
     * off itself.
     */
    @SubscribeEvent
    public static void onAddReloadListener(AddServerReloadListenersEvent event) {
        event.addListener(
                Identifier.fromNamespaceAndPath(PunchThemAll.MOD_ID, "interactions"),
                new InteractionReloadListener());
    }

    /**
     * Resolve the loaded specs once tags are bound. The reload listener itself runs too early: tag
     * contents are not available while resources load, so every {@code #tag} in a match would resolve
     * to nothing. This fires right after the tag bind and before the datapack sync below, so the
     * runtime registry is correct by the time clients are told about it.
     */
    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() != TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
            return; // The client rebuilds from the sync payload, not from its own tag update.
        }
        InteractionRegistry.getInstance().rebuildFrom(InteractionReloadListener.getLoaded(), event.getLookupProvider());
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        List<SyncInteractionsPayload> payloads = SyncInteractionsPayload.split(InteractionReloadListener.getLoaded());
        event.getRelevantPlayers().forEach(player ->
                payloads.forEach(payload -> PacketDistributor.sendToPlayer(player, payload)));
    }
}
