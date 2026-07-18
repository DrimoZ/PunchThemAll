package com.drimoz.punchthemall.core.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Server-side triggers that push the interaction registry to clients. Registered on the Forge bus.
 * Login covers a joining player; a reload broadcast (from the data reload listener) covers refreshes.
 */
public class PtaSyncEvents {

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PtaNetwork.syncTo(serverPlayer);
        }
    }
}
