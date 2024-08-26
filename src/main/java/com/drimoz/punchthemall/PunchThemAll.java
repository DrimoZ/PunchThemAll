package com.drimoz.punchthemall;

import com.drimoz.punchthemall.core.event.DataReloadListener;
import com.drimoz.punchthemall.core.event.PlayerInteractionHandler;
import com.drimoz.punchthemall.core.util.PTALoggers;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
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

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, PTAConfig.COMMON_CONFIG,  FILE_DESTINATION + "/" + MOD_ID + "-common.toml");

        MinecraftForge.EVENT_BUS.register(PlayerInteractionHandler.class);
        MinecraftForge.EVENT_BUS.addListener(this::addReloadListener);
        MinecraftForge.EVENT_BUS.register(this);
        PTALoggers.infoModCompleted();
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        PTALoggers.infoRegisteredModule("Common Setup Event");
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        PTALoggers.infoRegisteredModule("Client Setup Event");
    }

    public void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new DataReloadListener());
        PTALoggers.infoRegisteredModule("Add ReloadListener Event");
    }
}
