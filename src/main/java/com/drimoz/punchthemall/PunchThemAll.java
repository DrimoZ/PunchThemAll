package com.drimoz.punchthemall;

import com.drimoz.punchthemall.core.event.PlayerInteractionHandler;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
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
        modContainer.registerConfig(ModConfig.Type.CLIENT, PTAConfig.CLIENT_CONFIG, FILE_DESTINATION + "/" + MOD_ID + "-client.toml");

        // Runtime interaction handling (clicks, cooldowns). Interactions themselves are read from
        // datapacks by InteractionReloadListener and resolved into the runtime registry on
        // server/client via PtaServerEvents/PtaClientEvents. PTA registers no blocks or items, so
        // there is no DeferredRegister and nothing to do in common setup.
        NeoForge.EVENT_BUS.register(PlayerInteractionHandler.class);

        LOGGER.info("{} initialising", MOD_NAME);
    }
}
