package com.drimoz.punchthemall.core.event;

import com.drimoz.punchthemall.PunchThemAll;
import com.drimoz.punchthemall.core.model.classes.PtaInteraction;
import com.drimoz.punchthemall.core.registry.InteractionLoader;
import com.drimoz.punchthemall.core.registry.InteractionRegistry;
import com.drimoz.punchthemall.core.util.PTALoggers;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.network.chat.Component;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = PunchThemAll.MOD_ID)
public class DataReloadListener implements ResourceManagerReloadListener {

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        InteractionLoader.initInteractions();
        logInteractions();

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
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
