package com.drimoz.punchthemall;

import com.drimoz.punchthemall.core.event.DataReloadListener;
import com.drimoz.punchthemall.core.event.PlayerInteractionHandler;
import com.drimoz.punchthemall.core.util.PTALoggers;
import com.mojang.logging.LogUtils;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.ModContainer;
import org.slf4j.Logger;

@Mod(PunchThemAll.MOD_ID)
public class PunchThemAll
{
    public static final String MOD_ID = "pta";
    public static final String MOD_NAME = "PunchThemAll";
    public static final String FILE_DESTINATION = "punchthemall";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PunchThemAll(IEventBus modEventBus, ModContainer modContainer)
    {

        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::onClientSetup);

        modContainer.registerConfig(ModConfig.Type.COMMON, PTAConfig.COMMON_CONFIG,  FILE_DESTINATION + "/" + MOD_ID + "-common.toml");

        NeoForge.EVENT_BUS.register(PlayerInteractionHandler.class);
        NeoForge.EVENT_BUS.addListener(this::addReloadListener);
        NeoForge.EVENT_BUS.register(this);
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
