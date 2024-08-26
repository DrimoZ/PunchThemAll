package com.drimoz.punchthemall.core.event;

import com.drimoz.punchthemall.PunchThemAll;
import com.drimoz.punchthemall.core.model.classes.PtaInteraction;
import com.drimoz.punchthemall.core.registry.InteractionLoader;
import com.drimoz.punchthemall.core.registry.InteractionRegistry;
import com.drimoz.punchthemall.core.util.PTALoggers;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

@Mod.EventBusSubscriber(modid = PunchThemAll.MOD_ID)
public class ReloadEventHandler {
    private static boolean initialLoadDone = false;

    @SubscribeEvent
    public static void onDataReload(OnDatapackSyncEvent event) {
        if (initialLoadDone) {
            InteractionLoader.initInteractions();

            if (event.getPlayer() != null) {
                event.getPlayer().sendSystemMessage(Component.literal(PunchThemAll.MOD_NAME +  " Interactions have been reloaded!"));
            } else {
                for (ServerPlayer player : event.getPlayerList().getPlayers()) {
                    player.sendSystemMessage(Component.literal(PunchThemAll.MOD_NAME +  " Interactions have been reloaded!"));
                }
            }
        } else {
            InteractionLoader.initInteractions();
            initialLoadDone = true;
        }

        logInteractions();
    }

    private static void logInteractions() {
        for (PtaInteraction i : InteractionRegistry.getInstance().getInteractions().values()) {
            PTALoggers.info("New Interaction Registered : " + i.getId().getPath());
        }
        PTALoggers.info("Registered Interactions Count : " + InteractionRegistry.getInstance().getInteractions().values().size());
    }
}
