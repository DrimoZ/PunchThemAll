package com.drimoz.punchthemall.core.event;

import com.drimoz.punchthemall.PunchThemAll;
import com.drimoz.punchthemall.core.registry.InteractionRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;

/**
 * Client-side rebuild of the runtime interactions from the synchronised {@code pta:interaction}
 * datapack registry, then a JEI refresh. {@link RecipesUpdatedEvent} fires after the client receives
 * the server's data (world join, {@code /reload}), by which point the registry is available. This is
 * the only client hook needed — the datapack itself is synced by vanilla, so no custom networking.
 */
@EventBusSubscriber(modid = PunchThemAll.MOD_ID, value = Dist.CLIENT)
public class PtaClientEvents {

    @SubscribeEvent
    public static void onRecipesUpdated(RecipesUpdatedEvent event) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            InteractionRegistry.getInstance().rebuildFrom(connection.registryAccess());
        }

        // Push the refreshed registry into JEI (guarded so the class only loads when JEI is present).
        if (ModList.get().isLoaded("jei")) {
            com.drimoz.punchthemall.jei.JEIPlugin.refreshFromRegistry();
        }
    }
}
