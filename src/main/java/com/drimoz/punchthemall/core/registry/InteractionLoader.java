package com.drimoz.punchthemall.core.registry;

import com.drimoz.punchthemall.PTAConfig;
import com.drimoz.punchthemall.PunchThemAll;
import com.drimoz.punchthemall.core.model.classes.PtaInteraction;
import com.drimoz.punchthemall.core.util.PTALoggers;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public class InteractionLoader {

    public static void initInteractions() {
        InteractionRegistry.getInstance().clearInteractions();

        Path interactionsDir = FMLPaths.CONFIGDIR.get().resolve(PunchThemAll.FILE_DESTINATION).resolve("interactions");
        try {
            Files.createDirectories(interactionsDir);
        } catch (IOException e) {
            PTALoggers.error("Unable to create interaction directory " + interactionsDir + " : " + e);
            return;
        }

        int maxDepth = PTAConfig.LOADER.recursiveDiscovery.get() ? Integer.MAX_VALUE : 1;
        try (Stream<Path> paths = Files.walk(interactionsDir, maxDepth)) {
            paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(path -> interactionsDir.relativize(path).toString()))
                    .forEach(path -> loadInteraction(interactionsDir, path));
        } catch (IOException e) {
            PTALoggers.error("Unable to list interactions from " + interactionsDir + " : " + e);
        }
    }

    private static void loadInteraction(Path interactionsDir, Path file) {
        InputStreamReader reader = null;
        ResourceLocation id = null;
        PtaInteraction interaction = null;

        try {
            reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            id = createInteractionId(interactionsDir, file);
            interaction = InteractionCreator.createInteraction(id, json);
        } catch (Exception e) {
            String message = "An error occurred while creating interaction with id " + id + " from " + file + " : " + e;
            PTALoggers.error(message);
            if (PTAConfig.LOADER.failFast.get()) {
                throw new IllegalStateException(message, e);
            }
        } finally {
            IOUtils.closeQuietly(reader);
        }

        if (interaction != null) {
            InteractionRegistry.getInstance().addInteraction(interaction);
            if (PTAConfig.DEBUG.logLoadedInteractions.get()) {
                PTALoggers.info("Loaded PunchThemAll interaction " + interaction.getId());
            }
        }
    }

    private static ResourceLocation createInteractionId(Path interactionsDir, Path file) {
        String relativeName = interactionsDir.relativize(file).toString()
                .replace(file.getFileSystem().getSeparator(), "/")
                .replaceAll("\\.json$", "");
        if (PTAConfig.LOADER.lowerCaseGeneratedIds.get()) {
            relativeName = relativeName.toLowerCase();
        }
        return new ResourceLocation(PunchThemAll.MOD_ID, relativeName);
    }
}
