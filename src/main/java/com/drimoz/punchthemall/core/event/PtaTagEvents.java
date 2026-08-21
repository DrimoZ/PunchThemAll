package com.drimoz.punchthemall.core.event;

import com.drimoz.punchthemall.PunchThemAll;
import com.drimoz.punchthemall.core.model.classes.PtaInteraction;
import com.drimoz.punchthemall.core.network.PtaNetwork;
import com.drimoz.punchthemall.core.registry.InteractionParser;
import com.drimoz.punchthemall.core.registry.InteractionRegistry;
import com.drimoz.punchthemall.core.util.PTALoggers;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Resolves the loaded interactions a second time, once tags are bound.
 *
 * <p>Reload listeners run while resources load, and tag contents are not available yet at that
 * point: {@code ForgeRegistries.ITEMS.tags()} answers an empty set for every tag. So a selector
 * written as {@code "#minecraft:hoes"} resolved to nothing, and the interaction was loaded with an
 * empty item set — it appeared in JEI as a barrier and could never fire. Every tag-based selector in
 * the shipped examples was dead this way.</p>
 *
 * <p>Rather than move loading, this re-reads what the reload already kept. The registry stores the
 * raw JSON of every interaction for the client sync, so a second parse costs one pass over text
 * already in memory and needs nothing else to change.</p>
 */
@Mod.EventBusSubscriber(modid = PunchThemAll.MOD_ID)
public final class PtaTagEvents {

    private PtaTagEvents() {}

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        // A client rebuilds from the sync packet, which the server sends after this ran there.
        if (event.getUpdateCause() != TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) return;

        InteractionRegistry registry = InteractionRegistry.getInstance();
        Map<ResourceLocation, String> sources = new LinkedHashMap<>(registry.getSources());
        if (sources.isEmpty()) return;

        registry.clearInteractions();

        int resolved = 0;
        for (Map.Entry<ResourceLocation, String> entry : sources.entrySet()) {
            ResourceLocation id = entry.getKey();
            try {
                JsonObject json = JsonParser.parseString(entry.getValue()).getAsJsonObject();
                PtaInteraction interaction = InteractionParser.fromJson(id, json);
                if (interaction != null) {
                    registry.addInteraction(interaction, entry.getValue());
                    resolved++;
                }
            } catch (RuntimeException e) {
                PTALoggers.error("Could not resolve interaction " + id + " against the bound tags : " + e);
            }
        }

        PTALoggers.info("Resolved " + resolved + " interaction(s) against the bound tags");

        // The clients were told about the pre-tag set by the reload listener, so tell them again.
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
            PtaNetwork.syncToAll();
        }
    }
}
