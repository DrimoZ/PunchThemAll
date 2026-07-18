package com.drimoz.punchthemall.core.registry;

import com.drimoz.punchthemall.PTAConfig;
import com.drimoz.punchthemall.core.codec.InteractionSpec;
import com.drimoz.punchthemall.core.codec.InteractionSpecResolver;
import com.drimoz.punchthemall.core.model.classes.PtaInteraction;
import com.drimoz.punchthemall.core.util.PTALoggers;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

import static com.drimoz.punchthemall.core.registry.RegistryConstants.INCORRECT_FORMAT;
import static com.drimoz.punchthemall.core.registry.RegistryConstants.STRING_SCHEMA_VERSION;

/**
 * Single entry point that turns interaction JSON into a {@link PtaInteraction}, routing by
 * {@code schema_version}:
 * <ul>
 *     <li>&gt;= 2: the native, strictly-validated {@link InteractionSpec} codec pipeline.</li>
 *     <li>absent or 1: the legacy {@link InteractionCreator} (lenient JSON + regex SNBT), kept for
 *     backwards compatibility and emitting a one-time deprecation warning per file.</li>
 * </ul>
 * Both paths produce the same runtime model, so behaviour is identical; only the source format
 * and error reporting differ. Used by both the config loader and the datapack listener.
 */
public final class InteractionParser {

    public static final int CURRENT_SCHEMA_VERSION = 2;

    private InteractionParser() {}

    public static PtaInteraction fromJson(ResourceLocation id, JsonObject json) {
        int schemaVersion = readSchemaVersion(json);

        if (schemaVersion >= 2) {
            return fromSpec(id, json);
        }

        PTALoggers.warn("Interaction " + id.getPath() + " uses the legacy format (schema_version " + schemaVersion
                + "). Consider migrating to schema_version " + CURRENT_SCHEMA_VERSION + "; the legacy loader is deprecated.");
        return InteractionCreator.createInteraction(id, json);
    }

    private static PtaInteraction fromSpec(ResourceLocation id, JsonObject json) {
        DataResult<InteractionSpec> result = InteractionSpec.CODEC.parse(JsonOps.INSTANCE, json);

        Optional<InteractionSpec> parsed = result.result();
        if (parsed.isEmpty()) {
            String reason = result.error().map(DataResult.PartialResult::message).orElse("unknown error");
            PTALoggers.error(INCORRECT_FORMAT + " - " + id.getPath() + " - " + reason);
            return null;
        }

        InteractionSpec spec = parsed.get();
        if (!spec.enabled()) {
            if (PTAConfig.DEBUG.logLoadedInteractions.get()) {
                PTALoggers.info("Skipping disabled PunchThemAll interaction " + id);
            }
            return null;
        }

        return InteractionSpecResolver.resolve(id, spec);
    }

    private static int readSchemaVersion(JsonObject json) {
        if (json.has(STRING_SCHEMA_VERSION) && json.get(STRING_SCHEMA_VERSION).isJsonPrimitive()) {
            try {
                return json.getAsJsonPrimitive(STRING_SCHEMA_VERSION).getAsInt();
            } catch (NumberFormatException ignored) {
                // Fall through to legacy handling on a malformed schema_version.
            }
        }
        return 1;
    }
}
