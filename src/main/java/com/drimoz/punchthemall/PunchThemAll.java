package com.drimoz.punchthemall;

import com.drimoz.punchthemall.core.event.EventHandler;
import com.drimoz.punchthemall.core.model.Interaction;
import com.drimoz.punchthemall.core.registry.InteractionLoader;
import com.drimoz.punchthemall.core.registry.InteractionRegistry;
import com.drimoz.punchthemall.core.util.PTALoggers;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
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

        PTALoggers.infoModCompleted();
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        InteractionLoader.initInteractions();

        PTALoggers.infoRegisteredModule("Common Setup");
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(EventHandler.class);

        PTALoggers.infoRegisteredModule("Client Setup");
    }
}
