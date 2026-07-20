package com.drimoz.punchthemall.core.event;

import com.drimoz.punchthemall.PunchThemAll;
import com.drimoz.punchthemall.core.registry.InteractionRegistry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * Server-side rebuild of the runtime interactions from the {@code pta:interaction} datapack registry:
 * once on server start, and again after a {@code /reload} (the global {@link OnDatapackSyncEvent} with
 * no player). Clients rebuild independently from the synchronised registry (see PtaClientEvents).
 */
@EventBusSubscriber(modid = PunchThemAll.MOD_ID)
public class PtaServerEvents {

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        InteractionRegistry.getInstance().rebuildFrom(event.getServer().registryAccess());
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        // Player == null is the global reload broadcast; per-player joins don't change server data.
        if (event.getPlayer() == null) {
            InteractionRegistry.getInstance().rebuildFrom(event.getPlayerList().getServer().registryAccess());
        }
    }
}
