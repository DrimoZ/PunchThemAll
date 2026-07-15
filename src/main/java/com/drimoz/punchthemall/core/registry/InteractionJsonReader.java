package com.drimoz.punchthemall.core.registry;

import com.drimoz.punchthemall.core.model.records.PtaInteractionRecord;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.ArrayList;
import java.util.function.Consumer;

import static com.drimoz.punchthemall.core.registry.RegistryConstants.*;

public class InteractionJsonReader {

    private final ResourceLocation id;
    private final Consumer<String> errorReporter;

    public InteractionJsonReader(ResourceLocation id, Consumer<String> errorReporter) {
        this.id = id;
        this.errorReporter = errorReporter;
    }


    public boolean readBoolean(JsonObject parent, String key, boolean defaultValue) {
        if (!parent.has(key)) return defaultValue;
        if (!parent.get(key).isJsonPrimitive() || !parent.get(key).getAsJsonPrimitive().isBoolean()) {
            error(key + " must be a boolean");
            return defaultValue;
        }
        return GsonHelper.getAsBoolean(parent, key);
    }

    public Optional<PtaInteractionRecord> readInteractionRecord(JsonObject parent, String section) {
        if (!parent.has(section)) return Optional.empty();
        if (!parent.get(section).isJsonObject()) {
            error(section + " must be an object");
            return Optional.empty();
        }

        JsonObject json = GsonHelper.getAsJsonObject(parent, section);
        if (!hasPrimitive(json, STRING_CHANCE)) {
            missing(section + SEPARATOR + STRING_CHANCE);
            return Optional.empty();
        }

        double chance = GsonHelper.getAsDouble(json, STRING_CHANCE);
        Optional<Range> range = readRange(json, section, 1);
        return range.map(value -> new PtaInteractionRecord(chance, value.min(), value.max()));
    }

    public Optional<Range> readRange(JsonObject json, String path, int defaultMin) {
        if (hasPrimitive(json, STRING_COUNT)) {
            int count = GsonHelper.getAsInt(json, STRING_COUNT);
            return Optional.of(new Range(count, count));
        }

        if (!hasPrimitive(json, STRING_MIN)) {
            missing(path + SEPARATOR + STRING_MIN + " or " + path + SEPARATOR + STRING_COUNT);
            return Optional.empty();
        }

        int min = GsonHelper.getAsInt(json, STRING_MIN);
        int max = hasPrimitive(json, STRING_MAX) ? GsonHelper.getAsInt(json, STRING_MAX) : min;
        if (max < min) {
            error(path + SEPARATOR + STRING_MAX + " must be greater than or equal to " + path + SEPARATOR + STRING_MIN);
            return Optional.empty();
        }

        return Optional.of(new Range(Math.max(defaultMin, min), Math.max(defaultMin, max)));
    }

    public void readStringArray(JsonObject parent, String key, Set<String> output, String path) {
        if (!parent.has(key)) return;
        if (!parent.get(key).isJsonArray()) {
            error(path + " must be an array of strings");
            return;
        }

        JsonArray array = GsonHelper.getAsJsonArray(parent, key);
        for (JsonElement element : array) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                error(path + " contains a non-string value");
                continue;
            }
            String value = element.getAsString().trim();
            if (!value.isEmpty()) output.add(value);
        }
    }


    public List<String> readStringList(JsonObject parent, String key, String path) {
        List<String> values = new ArrayList<>();
        if (!parent.has(key)) return values;

        JsonElement value = parent.get(key);
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            addNonBlankString(values, value.getAsString(), path);
            return values;
        }

        if (!value.isJsonArray()) {
            error(path + " must be a string or an array of strings");
            return values;
        }

        JsonArray array = value.getAsJsonArray();
        for (JsonElement element : array) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                error(path + " contains a non-string value");
                continue;
            }
            addNonBlankString(values, element.getAsString(), path);
        }

        return values;
    }

    public boolean hasAny(JsonObject json, String... keys) {
        for (String key : keys) {
            if (json.has(key)) return true;
        }
        return false;
    }

    public boolean hasPrimitive(JsonObject json, String key) {
        return json.has(key) && json.get(key).isJsonPrimitive();
    }

    public boolean hasObject(JsonObject json, String key) {
        return json.has(key) && json.get(key).isJsonObject();
    }

    public boolean hasArray(JsonObject json, String key) {
        return json.has(key) && json.get(key).isJsonArray();
    }

    private void addNonBlankString(List<String> values, String rawValue, String path) {
        String value = rawValue.trim();
        if (value.isEmpty()) {
            error(path + " contains a blank string");
            return;
        }
        values.add(value);
    }

    private void missing(String path) {
        error("Missing " + path);
    }

    private void error(String message) {
        errorReporter.accept(INCORRECT_FORMAT + " - " + id.getPath() + " - " + message);
    }

    public record Range(int min, int max) {}
}
