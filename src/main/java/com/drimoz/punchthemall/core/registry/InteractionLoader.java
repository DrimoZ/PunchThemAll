package com.drimoz.punchthemall.core.registry;

import com.drimoz.punchthemall.PunchThemAll;
import com.drimoz.punchthemall.core.model.classes.PtaInteraction;
import com.drimoz.punchthemall.core.util.PTALoggers;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;

import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class InteractionLoader {

    public static void initInteractions() {
        InteractionRegistry.getInstance().clearInteractions();

        var dir = FMLPaths.CONFIGDIR.get().resolve(PunchThemAll.FILE_DESTINATION + "/interactions").toFile();
        if (!dir.exists() && dir.mkdirs()) {
            PTALoggers.info("Created /config/" + PunchThemAll.FILE_DESTINATION + "/interactions/ directory");
        }

        var files = dir.listFiles((FileFilter) FileFilterUtils.suffixFileFilter(".json"));
        if (files == null) return;

        for (var file : files) {
            JsonObject json;
            InputStreamReader reader = null;
            ResourceLocation id = null;
            PtaInteraction interaction = null;

            try {
                reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
                json = JsonParser.parseReader(reader).getAsJsonObject();
                var name = file.getName().replace(".json", "");
                id = new ResourceLocation(PunchThemAll.MOD_ID, name);

                interaction = InteractionCreator.createInteraction(id, json);

                reader.close();
            } catch (Exception e) {
               PTALoggers.error("An error occurred while creating interaction with id " + id + " : " + e);
            } finally {
                IOUtils.closeQuietly(reader);
            }

            if (interaction != null)
                InteractionRegistry.getInstance().addInteraction(interaction);
        }
    }
}
