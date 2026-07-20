package com.drimoz.punchthemall.core.event;

import com.drimoz.punchthemall.PunchThemAll;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;

/**
 * The interactions themselves arrive via {@code SyncInteractionsPayload}, which also refreshes the
 * viewers. This hook exists because JEI tears its runtime down and restarts it on
 * {@link RecipesUpdatedEvent}: if the payload landed before that restart, the fresh runtime would
 * come up empty. Re-pushing here is idempotent and covers whichever order the two arrive in.
 */
@EventBusSubscriber(modid = PunchThemAll.MOD_ID, value = Dist.CLIENT)
public class PtaClientEvents {

    @SubscribeEvent
    public static void onRecipesUpdated(RecipesUpdatedEvent event) {
        if (ModList.get().isLoaded("jei")) {
            com.drimoz.punchthemall.jei.JEIPlugin.refreshFromRegistry();
        }
    }
}
