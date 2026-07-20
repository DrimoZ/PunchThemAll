package com.drimoz.punchthemall.core.event;

import com.drimoz.punchthemall.PunchThemAll;
import com.drimoz.punchthemall.core.codec.InteractionSpec;
import com.drimoz.punchthemall.core.registry.PtaRegistries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/**
 * Declares the {@code pta:interaction} datapack registry. The same {@link InteractionSpec} codec is
 * used to load the JSON and to sync it to clients (the network codec argument), so interactions
 * travel to clients for free via vanilla datapack synchronisation.
 */
@EventBusSubscriber(modid = PunchThemAll.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class PtaRegistryEvents {

    @SubscribeEvent
    public static void onNewDataPackRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(PtaRegistries.INTERACTION, InteractionSpec.CODEC, InteractionSpec.CODEC);
    }
}
