package com.drimoz.punchthemall.core.event;

import com.drimoz.punchthemall.PunchThemAll;
import com.drimoz.punchthemall.core.network.LeftClickEmptyPacket;
import com.drimoz.punchthemall.core.network.PtaNetwork;
import com.drimoz.punchthemall.core.registry.InteractionRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-only listeners.
 *
 * <p>One, and it exists because a left click on nothing is the single click type the server never
 * sees. Everything else in PunchThemAll happens on the server.</p>
 */
@Mod.EventBusSubscriber(modid = PunchThemAll.MOD_ID, value = Dist.CLIENT)
public final class PtaClientEvents {

    private PtaClientEvents() {}

    /**
     * Forward a left click on nothing to the server, which never sees this event (see
     * {@link LeftClickEmptyPacket}). Gated on the loaded pack actually having such an interaction,
     * so swinging at air stays free for everyone else.
     */
    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        if (InteractionRegistry.getInstance().hasLeftClickAirInteraction()) {
            PtaNetwork.sendLeftClickEmpty();
        }
    }
}
