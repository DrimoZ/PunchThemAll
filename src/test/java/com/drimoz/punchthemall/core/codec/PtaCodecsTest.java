package com.drimoz.punchthemall.core.codec;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The shared codecs the v2 format is built from: selectors, coerced scalars, SNBT. */
class PtaCodecsTest {

    private static <T> T parse(com.mojang.serialization.Codec<T> codec, String json) {
        return codec.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .getOrThrow(message -> new AssertionError(message));
    }

    @Test
    @DisplayName("a selector accepts a single string")
    void selectorFromString() {
        assertEquals(List.of("minecraft:stone"), parse(PtaCodecs.STRING_OR_LIST, "\"minecraft:stone\""));
    }

    @Test
    @DisplayName("a selector accepts a list")
    void selectorFromList() {
        assertEquals(List.of("minecraft:stone", "#c:ores"),
                parse(PtaCodecs.STRING_OR_LIST, "[\"minecraft:stone\", \"#c:ores\"]"));
    }

    @Test
    @DisplayName("a selector accepts an empty list")
    void selectorFromEmptyList() {
        assertEquals(List.of(), parse(PtaCodecs.STRING_OR_LIST, "[]"));
    }

    @Test
    @DisplayName("a single-entry selector encodes back to a bare string")
    void selectorEncodesCompactly() {
        JsonElement encoded = PtaCodecs.STRING_OR_LIST
                .encodeStart(JsonOps.INSTANCE, List.of("minecraft:stone"))
                .getOrThrow(message -> new AssertionError(message));

        assertTrue(encoded.isJsonPrimitive());
        assertEquals("minecraft:stone", encoded.getAsString());
    }

    @Test
    @DisplayName("a multi-entry selector encodes back to a list")
    void selectorEncodesAsList() {
        JsonElement encoded = PtaCodecs.STRING_OR_LIST
                .encodeStart(JsonOps.INSTANCE, List.of("a", "b"))
                .getOrThrow(message -> new AssertionError(message));

        assertTrue(encoded.isJsonArray());
    }

    @Test
    @DisplayName("a scalar coerces strings, booleans and numbers alike")
    void scalarCoercion() {
        // Block state values are strings in the model but authors write `true` and `4` unquoted.
        assertEquals("north", parse(PtaCodecs.SCALAR_STRING, "\"north\""));
        assertEquals("true", parse(PtaCodecs.SCALAR_STRING, "true"));
        assertEquals("false", parse(PtaCodecs.SCALAR_STRING, "false"));
        assertEquals("4", parse(PtaCodecs.SCALAR_STRING, "4"));
    }

    @Test
    @DisplayName("SNBT parses to a compound")
    void snbtParses() {
        CompoundTag tag = parse(PtaCodecs.SNBT, "\"{Damage:5,custom:{tier:2}}\"");

        assertEquals(5, tag.getIntOr("Damage", -1));
        assertEquals(2, tag.getCompoundOrEmpty("custom").getIntOr("tier", -1));
    }

    @Test
    @DisplayName("SNBT round-trips through its string form")
    void snbtRoundTrips() {
        CompoundTag original = parse(PtaCodecs.SNBT, "\"{Damage:5,name:\\\"pick\\\"}\"");

        JsonElement encoded = PtaCodecs.SNBT.encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow(message -> new AssertionError(message));

        assertEquals(original, parse(PtaCodecs.SNBT, encoded.toString()));
    }

    @Test
    @DisplayName("malformed SNBT is a readable error, not an exception")
    void snbtReportsSyntaxErrors() {
        var result = PtaCodecs.SNBT.parse(JsonOps.INSTANCE, JsonParser.parseString("\"{Damage:\""));

        assertTrue(result.isError());
        assertTrue(result.error().orElseThrow().message().contains("Invalid SNBT"));
    }

    @Test
    @DisplayName("SNBT must be a string, not an inline JSON object")
    void snbtRejectsObjects() {
        assertTrue(PtaCodecs.SNBT.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"Damage\": 5}")).isError());
    }

    @Test
    @DisplayName("a state map coerces its values")
    void stateMapCoercesValues() {
        Map<String, String> states = parse(
                com.mojang.serialization.Codec.unboundedMap(com.mojang.serialization.Codec.STRING, PtaCodecs.SCALAR_STRING),
                "{\"waterlogged\": true, \"level\": 3, \"facing\": \"north\"}");

        assertEquals("true", states.get("waterlogged"));
        assertEquals("3", states.get("level"));
        assertEquals("north", states.get("facing"));
    }
}
