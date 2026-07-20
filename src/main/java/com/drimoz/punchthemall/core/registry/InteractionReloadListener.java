package com.drimoz.punchthemall.core.registry;

import com.drimoz.punchthemall.PTAConfig;
import com.drimoz.punchthemall.core.codec.InteractionSpec;
import com.drimoz.punchthemall.core.util.PTALoggers;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import net.neoforged.neoforge.common.conditions.ICondition;

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
 */
public class InteractionReloadListener extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "pta/interaction";
    private static final Gson GSON = new Gson();

    // Last successfully loaded set, kept so the sync payload can be rebuilt for any joining player.
    private static Map<ResourceLocation, InteractionSpec> loaded = Map.of();

    private final HolderLookup.Provider registries;
    private final ICondition.IContext conditionContext;

    public InteractionReloadListener(HolderLookup.Provider registries, ICondition.IContext conditionContext) {
        super(GSON, DIRECTORY);
        this.registries = registries;
        this.conditionContext = conditionContext;
    }

    /** The specs loaded by the most recent reload, for syncing to clients. */
    public static Map<ResourceLocation, InteractionSpec> getLoaded() {
        return loaded;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        // ConditionalOps gives files access to neoforge:conditions; RegistryOps lets specs reference
        // registry contents. Both are lost if we fall back to plain JsonOps, hence the wrapping here.
        DynamicOps<JsonElement> ops = new ConditionalOps<>(RegistryOps.create(JsonOps.INSTANCE, registries), conditionContext);
        Codec<Optional<InteractionSpec>> codec = ConditionalOps.createConditionalCodec(InteractionSpec.CODEC);

        Map<ResourceLocation, InteractionSpec> specs = new HashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> file : files.entrySet()) {
            ResourceLocation id = file.getKey();
            DataResult<Optional<InteractionSpec>> result = codec.parse(ops, file.getValue());

            Optional<Optional<InteractionSpec>> parsed = result.result();
            if (parsed.isEmpty()) {
                String error = result.error().map(DataResult.Error::message).orElse("unknown error");
                PTALoggers.error(RegistryConstants.INCORRECT_FORMAT + " - " + id + " - " + error);
                continue;
            }

            // Empty means the file's conditions were not met — a deliberate skip, not a failure.
            parsed.get().ifPresent(spec -> specs.put(id, spec));
        }

        loaded = Map.copyOf(specs);
        InteractionRegistry.getInstance().rebuildFrom(loaded, registries);

        if (PTAConfig.DEBUG.logLoadedInteractions.get()) {
            PTALoggers.info("Read " + files.size() + " interaction file(s) from datapacks");
        }
    }
}
