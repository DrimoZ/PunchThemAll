package com.drimoz.punchthemall.core.event;

import com.drimoz.punchthemall.PunchThemAll;
import com.drimoz.punchthemall.core.network.LeftClickEmptyPayload;
import com.drimoz.punchthemall.core.registry.InteractionRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Client-only hooks: keeping the viewers in step, and reporting clicks the server cannot see. */
@EventBusSubscriber(modid = PunchThemAll.MOD_ID, value = Dist.CLIENT)
public class PtaClientEvents {

    @SubscribeEvent
    public static void onRecipesUpdated(RecipesUpdatedEvent event) {
        if (ModList.get().isLoaded("jei")) {
            com.drimoz.punchthemall.jei.JEIPlugin.refreshFromRegistry();
        }
    }

    /**
     * Forward a left click on nothing to the server, which never sees this event (see
     * {@link LeftClickEmptyPayload}). Gated on the pack actually having such an interaction so that
     * swinging at air stays free for everyone else.
     */
    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        if (InteractionRegistry.getInstance().hasLeftClickAirInteraction()) {
            PacketDistributor.sendToServer(LeftClickEmptyPayload.INSTANCE);
        }
    }
}
