package com.drimoz.punchthemall.core.event;

import com.drimoz.punchthemall.PunchThemAll;
import com.drimoz.punchthemall.core.network.LeftClickEmptyPayload;
import com.drimoz.punchthemall.core.network.PtaNetwork;
import com.drimoz.punchthemall.core.registry.InteractionRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Client-only hooks: keeping the viewers in step, and reporting clicks the server cannot see. */
@EventBusSubscriber(modid = PunchThemAll.MOD_ID, value = Dist.CLIENT)
public class PtaClientEvents {

    @SubscribeEvent
    public static void onRecipesReceived(RecipesReceivedEvent event) {
        PtaNetwork.refreshViewers();
    }

    /**
     * Forget the server's interactions on the way out.
     *
     * <p>The registry is a singleton that outlives the connection, so without this a client keeps
     * showing the previous server's interactions in JEI — on the main menu, and until the next
     * server's sync lands. It also matters for gameplay in singleplayer, where the client-side
     * rebuild writes to the very same registry the integrated server reads.</p>
     */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        PtaNetwork.clearPending();
        InteractionRegistry.getInstance().clearInteractions();
        PtaNetwork.refreshViewers();
    }

    /**
     * Forward a left click on nothing to the server, which never sees this event (see
     * {@link LeftClickEmptyPayload}). Gated on the pack actually having such an interaction so that
     * swinging at air stays free for everyone else.
     */
    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        if (InteractionRegistry.getInstance().hasLeftClickAirInteraction()) {
            ClientPacketDistributor.sendToServer(LeftClickEmptyPayload.INSTANCE);
        }
    }
}
