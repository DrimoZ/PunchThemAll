package com.drimoz.punchthemall.core.event;

import com.drimoz.punchthemall.PunchThemAll;
import com.drimoz.punchthemall.core.network.SyncInteractionsPayload;
import com.drimoz.punchthemall.core.registry.InteractionReloadListener;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Server-side loading and syncing of interactions.
 *
 * <p>{@link AddReloadListenerEvent} fires both at server start and on {@code /reload}, so one
 * listener covers both. {@link OnDatapackSyncEvent} then fires for the joining player, or for every
 * player after a reload — {@code getRelevantPlayers()} already encodes that distinction, so a single
 * handler serves both cases.</p>
 */
@EventBusSubscriber(modid = PunchThemAll.MOD_ID)
public class PtaServerEvents {

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new InteractionReloadListener(event.getRegistryAccess(), event.getConditionContext()));
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        SyncInteractionsPayload payload = new SyncInteractionsPayload(InteractionReloadListener.getLoaded());
        event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, payload));
    }
}
