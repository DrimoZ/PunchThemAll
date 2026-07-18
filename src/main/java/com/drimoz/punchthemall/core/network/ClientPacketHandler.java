package com.drimoz.punchthemall.core.network;

import com.drimoz.punchthemall.core.model.classes.PtaInteraction;
import com.drimoz.punchthemall.core.registry.InteractionParser;
import com.drimoz.punchthemall.core.registry.InteractionRegistry;
import com.drimoz.punchthemall.core.util.PTALoggers;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;

import java.util.Map;

/**
 * Client-only sink for {@link SyncInteractionsPacket}. Isolated so the server never class-loads
 * {@link Minecraft}. Rebuilds the client registry from the server's raw JSON so JEI is accurate.
 */
public final class ClientPacketHandler {

    private ClientPacketHandler() {}

    public static void applySync(SyncInteractionsPacket packet) {
        // Single-player / LAN host: client and server share the same registry singleton in this JVM,
        // and the server has already populated it. Re-parsing would be redundant, so short-circuit.
        if (Minecraft.getInstance().getSingleplayerServer() != null) {
            return;
        }

        InteractionRegistry registry = InteractionRegistry.getInstance();
        registry.clearInteractions();

        int loaded = 0;
        for (Map.Entry<ResourceLocation, String> entry : packet.sources().entrySet()) {
            ResourceLocation id = entry.getKey();
            try {
                JsonObject json = JsonParser.parseString(entry.getValue()).getAsJsonObject();
                PtaInteraction interaction = InteractionParser.fromJson(id, json);
                if (interaction != null) {
                    registry.addInteraction(interaction, entry.getValue());
                    loaded++;
                }
            } catch (Exception e) {
                PTALoggers.error("Failed to apply synchronised interaction " + id + " : " + e);
            }
        }

        PTALoggers.info("Synchronised " + loaded + " interaction(s) from the server");

        // Push the refreshed registry into JEI's runtime so the category reflects the server.
        // Guarded so the class is only loaded when JEI is present.
        if (ModList.get().isLoaded("jei")) {
            com.drimoz.punchthemall.jei.JEIPlugin.refreshFromRegistry();
        }
    }
}
