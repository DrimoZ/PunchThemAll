package com.drimoz.punchthemall.core.codec;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The three JSON shapes {@code count} accepts, and the per-context floor. */
class CountSpecTest {

    private static CountSpec parse(String json) {
        return CountSpec.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .getOrThrow(message -> new AssertionError(message));
    }

    @Test
    @DisplayName("a bare integer is an exact count")
    void bareInteger() {
        assertEquals(new CountSpec.Range(3, 3), parse("3").resolve(0));
    }

    @Test
    @DisplayName("{count: n} is an exact count")
    void countObject() {
        assertEquals(new CountSpec.Range(3, 3), parse("{\"count\": 3}").resolve(0));
    }

    @Test
    @DisplayName("{min, max} is a range")
    void minMax() {
        assertEquals(new CountSpec.Range(1, 3), parse("{\"min\": 1, \"max\": 3}").resolve(0));
    }

    @Test
    @DisplayName("max defaults to min when absent")
    void maxDefaultsToMin() {
        assertEquals(new CountSpec.Range(2, 2), parse("{\"min\": 2}").resolve(0));
    }

    @Test
    @DisplayName("max below min is raised to min rather than producing an inverted range")
    void invertedRangeIsClamped() {
        assertEquals(new CountSpec.Range(5, 5), parse("{\"min\": 5, \"max\": 2}").resolve(0));
    }

    @Test
    @DisplayName("the floor lifts both bounds: 0 for drop pools, 1 for player costs")
    void floorApplies() {
        // A pool tolerates zero; a cost must take at least one point, or it would be a no-op that
        // still reads as configured.
        assertEquals(new CountSpec.Range(0, 3), parse("{\"min\": 0, \"max\": 3}").resolve(0));
        assertEquals(new CountSpec.Range(1, 3), parse("{\"min\": 0, \"max\": 3}").resolve(1));
        assertEquals(new CountSpec.Range(1, 1), parse("0").resolve(1));
    }

    @Test
    @DisplayName("an empty object falls back to the floor")
    void emptyObject() {
        assertEquals(new CountSpec.Range(0, 0), parse("{}").resolve(0));
        assertEquals(new CountSpec.Range(1, 1), parse("{}").resolve(1));
    }

    @Test
    @DisplayName("encoding round-trips back to a readable shape")
    void roundTrip() {
        CountSpec range = parse("{\"min\": 1, \"max\": 4}");
        var encoded = CountSpec.CODEC.encodeStart(JsonOps.INSTANCE, range)
                .getOrThrow(message -> new AssertionError(message));

        assertEquals(new CountSpec.Range(1, 4),
                CountSpec.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow(AssertionError::new).resolve(0));
    }

    @Test
    @DisplayName("a non-numeric count is rejected")
    void rejectsGarbage() {
        assertTrue(CountSpec.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("\"three\"")).isError());
    }
}
