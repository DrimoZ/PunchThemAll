package com.drimoz.punchthemall.core.event;

import com.drimoz.punchthemall.PunchThemAll;
import com.drimoz.punchthemall.core.model.classes.PtaInteraction;
import com.drimoz.punchthemall.core.network.PtaNetwork;
import com.drimoz.punchthemall.core.registry.DatapackInteractionLoader;
import com.drimoz.punchthemall.core.registry.InteractionLoader;
import com.drimoz.punchthemall.core.registry.InteractionRegistry;
import com.drimoz.punchthemall.core.util.PTALoggers;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.network.chat.Component;
import net.minecraftforge.server.ServerLifecycleHooks;

public class DataReloadListener implements ResourceManagerReloadListener {

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        // Config files first (they clear the registry), then optional datapack files layered on top
        // so a datapack interaction overrides a config one sharing the same id.
        InteractionLoader.initInteractions();
        DatapackInteractionLoader.loadFromDatapacks(resourceManager);
        logInteractions();

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            // Push the freshly loaded registry to every client so JEI stays in sync (no-op in solo).
            PtaNetwork.syncToAll();

            Component chatMessage = Component.literal(PunchThemAll.MOD_NAME + " Interactions Reloaded !");
            server.getPlayerList().broadcastSystemMessage(chatMessage, false);
        }
    }

    private static void logInteractions() {
        for (PtaInteraction i : InteractionRegistry.getInstance().getInteractions().values()) {
            PTALoggers.info("New Interaction Registered : " + i.getId().getPath());
        }
        PTALoggers.info("Registered Interactions Count : " + InteractionRegistry.getInstance().getInteractions().values().size());
    }


}
