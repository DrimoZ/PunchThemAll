package com.drimoz.punchthemall;

import com.drimoz.punchthemall.core.event.PlayerInteractionHandler;
import com.drimoz.punchthemall.core.model.classes.PtaInteraction;
import com.drimoz.punchthemall.core.registry.InteractionLoader;
import com.drimoz.punchthemall.core.registry.InteractionRegistry;
import com.drimoz.punchthemall.core.util.PTALoggers;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(PunchThemAll.MOD_ID)
public class PunchThemAll
{
    public static final String MOD_ID = "pta";
    public static final String MOD_NAME = "PunchThemAll";
    public static final String FILE_DESTINATION = "punchthemall";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PunchThemAll()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::onClientSetup);

        MinecraftForge.EVENT_BUS.register(this);
        PTALoggers.infoModCompleted();
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        PTALoggers.infoRegisteredModule("Common Setup");
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        PTALoggers.infoRegisteredModule("Client Setup");
    }

    @SubscribeEvent
    public void onServerStartup(ServerStartingEvent event) {
        InteractionLoader.initInteractions();

        for (PtaInteraction i : InteractionRegistry.getInstance().getInteractions().values()) {
            PTALoggers.info("New Interaction Registered : " + i.getId().getPath());
        }
        PTALoggers.info("Registered Interactions Count : " + InteractionRegistry.getInstance().getInteractions().values().size());

        PTALoggers.infoRegisteredModule("Server Starting");

    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        MinecraftForge.EVENT_BUS.register(PlayerInteractionHandler.class);

        PTALoggers.infoRegisteredModule("Server Started");

    }
}
