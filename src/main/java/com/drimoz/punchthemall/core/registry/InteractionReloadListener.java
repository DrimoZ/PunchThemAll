package com.drimoz.punchthemall.core.registry;

import com.drimoz.punchthemall.PTAConfig;
import com.drimoz.punchthemall.core.codec.InteractionSpec;
import com.drimoz.punchthemall.core.util.PTALoggers;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.common.conditions.ConditionalOps;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Loads {@code data/<namespace>/pta/interaction/*.json} as part of the server's reloadable
 * resources.
 *
 * <p>Interactions deliberately are <b>not</b> a datapack registry. A datapack registry syncs to
 * clients for free, but {@code MinecraftServer.reloadResources} reuses its {@code registries} field
 * untouched, so a registry is only ever read when the world loads — {@code /reload} could never pick
 * up an edited interaction. Being an ordinary reload listener restores the
 * "edit the JSON, {@code /reload}, test" loop; clients are kept in step by an explicit sync payload
 * sent from {@code PtaServerEvents} on {@code OnDatapackSyncEvent} (which fires both on join and
 * after a reload).</p>
 *
 * <p>This extends {@link SimplePreparableReloadListener} rather than
 * {@code SimpleJsonResourceReloadListener}, which since 1.21.4 decodes every file itself using a
 * codec fixed at construction. That would be shorter, but a bad file is then reported in vanilla's
 * words and dropped — and per-file error reporting in PTA's own format is a feature the 2.2.0 audit
 * added deliberately and verified in game. Decoding here keeps it. NeoForge makes
 * {@link SimplePreparableReloadListener} a {@code ContextAwareReloadListener}, so the registry
 * lookup and the condition context arrive by injection and {@link #makeConditionalOps()} builds
 * exactly the ops this needs — no constructor plumbing.</p>
 */
public class InteractionReloadListener extends SimplePreparableReloadListener<Map<Identifier, InteractionSpec>> {

    public static final String DIRECTORY = "pta/interaction";

    private static final FileToIdConverter FINDER = FileToIdConverter.json(DIRECTORY);

    // Last successfully loaded set, kept so the sync payload can be rebuilt for any joining player.
    private static Map<Identifier, InteractionSpec> loaded = Map.of();

    /** The specs loaded by the most recent reload, for syncing to clients. */
    public static Map<Identifier, InteractionSpec> getLoaded() {
        return loaded;
    }

    /**
     * Read and decode every interaction file. This runs off the main thread, so it only builds the
     * map; publishing it is {@link #apply}'s job.
     */
    @Override
    protected Map<Identifier, InteractionSpec> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        // ConditionalOps gives files access to neoforge:conditions; the RegistryOps it wraps lets
        // specs reference registry contents. Both are lost if we fall back to plain JsonOps.
        DynamicOps<JsonElement> ops = makeConditionalOps();
        Codec<Optional<InteractionSpec>> codec = ConditionalOps.createConditionalCodec(InteractionSpec.CODEC);

        Map<Identifier, InteractionSpec> specs = new HashMap<>();

        for (Map.Entry<Identifier, Resource> file : FINDER.listMatchingResources(resourceManager).entrySet()) {
            Identifier id = FINDER.fileToId(file.getKey());

            JsonElement json;
            try (BufferedReader reader = file.getValue().openAsReader()) {
                json = JsonParser.parseReader(reader);
            } catch (IOException | RuntimeException e) {
                // Unreadable or syntactically broken: report and skip, exactly as a failed decode
                // does. One bad file must not cost the pack every other interaction.
                PTALoggers.error(RegistryConstants.INCORRECT_FORMAT + " - " + id + " - " + e);
                continue;
            }

            DataResult<Optional<InteractionSpec>> result = codec.parse(ops, json);

            Optional<Optional<InteractionSpec>> parsed = result.result();
            if (parsed.isEmpty()) {
                String error = result.error().map(DataResult.Error::message).orElse("unknown error");
                PTALoggers.error(RegistryConstants.INCORRECT_FORMAT + " - " + id + " - " + error);
                continue;
            }

            // Empty means the file's conditions were not met — a deliberate skip, not a failure.
            parsed.get().ifPresent(spec -> specs.put(id, spec));
        }

        return specs;
    }

    @Override
    protected void apply(Map<Identifier, InteractionSpec> specs, ResourceManager resourceManager, ProfilerFiller profiler) {
        // Only the specs are published here. Resolving them into the runtime model looks tags up in
        // BuiltInRegistries, and reload listeners run before tags are bound — resolving now would
        // silently give every #tag an empty item/block set. PtaServerEvents finishes the job on
        // TagsUpdatedEvent instead.
        loaded = Map.copyOf(specs);

        if (PTAConfig.valueOrDefault(PTAConfig.DEBUG.logLoadedInteractions)) {
            PTALoggers.info("Read " + specs.size() + " interaction file(s) from datapacks");
        }
    }
}
