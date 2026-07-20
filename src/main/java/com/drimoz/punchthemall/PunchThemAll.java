package com.drimoz.punchthemall;

import com.drimoz.punchthemall.core.event.PlayerInteractionHandler;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * PunchThemAll — NeoForge 1.21.1 port.
 */
@Mod(PunchThemAll.MOD_ID)
public class PunchThemAll {

    public static final String MOD_ID = "pta";
    public static final String MOD_NAME = "PunchThemAll";
    public static final String FILE_DESTINATION = "punchthemall";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PunchThemAll(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, PTAConfig.COMMON_CONFIG, FILE_DESTINATION + "/" + MOD_ID + "-common.toml");

        modEventBus.addListener(this::onCommonSetup);

        // Runtime interaction handling (clicks, cooldowns). Interactions themselves are loaded from
        // the pta:interaction datapack registry (see PtaRegistries) and resolved into the runtime
        // registry on server/client via PtaServerEvents/PtaClientEvents.
        NeoForge.EVENT_BUS.register(PlayerInteractionHandler.class);

        LOGGER.info("{} initialising (NeoForge 1.21.1 port)", MOD_NAME);
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        // Registration and wiring are added in later port phases.
    }
}
