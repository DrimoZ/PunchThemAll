package com.drimoz.punchthemall.core.registry;

import com.drimoz.punchthemall.PTAConfig;
import com.drimoz.punchthemall.core.model.classes.PtaInteraction;
import com.drimoz.punchthemall.core.util.PTALoggers;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.io.IOUtils;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Opt-in datapack source for interactions, layered on top of the config directory.
 *
 * <p>Reads {@code data/<namespace>/pta/interaction/*.json} from the server's {@link ResourceManager}
 * and adds them to the (already config-populated) registry. This piggybacks on vanilla's datapack
 * handling, so files are merged/overridden across packs and synchronised to clients for free.
 * Gated by {@code LOADER.load_from_datapacks}. Both schema versions are supported via
 * {@link InteractionParser}.</p>
 */
public final class DatapackInteractionLoader {

    public static final String DIRECTORY = "pta/interaction";
    private static final String EXTENSION = ".json";

    private DatapackInteractionLoader() {}

    public static void loadFromDatapacks(ResourceManager resourceManager) {
        if (!PTAConfig.LOADER.loadFromDatapacks.get()) {
            return;
        }

        Map<ResourceLocation, Resource> resources =
                resourceManager.listResources(DIRECTORY, location -> location.getPath().endsWith(EXTENSION));

        int loaded = 0;
        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation id = toInteractionId(entry.getKey());
            if (loadOne(id, entry.getValue())) {
                loaded++;
            }
        }

        if (loaded > 0 || PTAConfig.DEBUG.logLoadedInteractions.get()) {
            PTALoggers.info("Loaded " + loaded + " interaction(s) from datapacks");
        }
    }

    private static boolean loadOne(ResourceLocation id, Resource resource) {
        try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            PtaInteraction interaction = InteractionParser.fromJson(id, json);
            if (interaction != null) {
                InteractionRegistry.getInstance().addInteraction(interaction, json.toString());
                if (PTAConfig.DEBUG.logLoadedInteractions.get()) {
                    PTALoggers.info("Loaded PunchThemAll datapack interaction " + interaction.getId());
                }
                return true;
            }
        } catch (Exception e) {
            String message = "An error occurred while loading datapack interaction " + id + " : " + e;
            PTALoggers.error(message);
            if (PTAConfig.LOADER.failFast.get()) {
                throw new IllegalStateException(message, e);
            }
        }
        return false;
    }

    // data/<ns>/pta/interaction/foo/bar.json  ->  <ns>:foo/bar
    private static ResourceLocation toInteractionId(ResourceLocation resourceKey) {
        String path = resourceKey.getPath();
        path = path.substring(DIRECTORY.length() + 1, path.length() - EXTENSION.length());
        return new ResourceLocation(resourceKey.getNamespace(), path);
    }
}
