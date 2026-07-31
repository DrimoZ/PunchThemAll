package com.drimoz.punchthemall.core.codec;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Parsing of the schema_version 2 file shape: defaults, required fields, and the whole thing. */
class InteractionSpecTest {

    private static InteractionSpec parse(String json) {
        return InteractionSpec.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .getOrThrow(message -> new AssertionError(message));
    }

    private static DataResult<InteractionSpec> tryParse(String json) {
        return InteractionSpec.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json));
    }

    @Test
    @DisplayName("type is the only required field")
    void minimalFile() {
        InteractionSpec spec = parse("{\"type\": \"right_click\"}");

        assertEquals("right_click", spec.type());
        assertEquals(2, spec.schemaVersion());
        assertTrue(spec.enabled());
        assertFalse(spec.hidden());
        assertTrue(spec.hand().isEmpty());
        assertTrue(spec.target().isEmpty());
        assertTrue(spec.rewards().isEmpty());
        assertTrue(spec.effects().isEmpty());
    }

    @Test
    @DisplayName("a missing type is rejected")
    void typeRequired() {
        assertTrue(tryParse("{}").isError());
    }

    @Test
    @DisplayName("an explicit schema_version is kept, so an old file can be told apart")
    void schemaVersionKept() {
        assertEquals(1, parse("{\"type\": \"right_click\", \"schema_version\": 1}").schemaVersion());
    }

    @Test
    @DisplayName("hidden defaults to false and is read when present")
    void hiddenFlag() {
        assertFalse(parse("{\"type\": \"left_click\"}").hidden());
        assertTrue(parse("{\"type\": \"left_click\", \"hidden\": true}").hidden());
        assertFalse(parse("{\"type\": \"left_click\", \"hidden\": false}").hidden());
    }

    @Test
    @DisplayName("hidden and enabled are independent")
    void hiddenIsNotEnabled() {
        InteractionSpec spec = parse("{\"type\": \"left_click\", \"hidden\": true, \"enabled\": true}");

        assertTrue(spec.enabled());
        assertTrue(spec.hidden());
    }

    @Test
    @DisplayName("hand defaults: any hand, no match, consume none")
    void handDefaults() {
        InteractionSpec spec = parse("{\"type\": \"right_click\", \"hand\": {}}");
        InteractionSpec.HandSpec hand = spec.hand().orElseThrow();

        assertEquals("any", hand.hand());
        assertEquals(List.of(), hand.match());
        assertEquals("none", hand.consume().mode());
        assertEquals(1.0, hand.consume().chance());
        assertEquals(1, hand.consume().count().resolve(1).min());
        assertEquals(1, hand.consume().count().resolve(1).max());
        assertTrue(hand.nbtPredicates().isEmpty());
    }

    @Test
    @DisplayName("consume count accepts a plain int and a min/max range")
    void consumeCount() {
        InteractionSpec.ConsumeSpec exact = parse("""
                {"type": "right_click", "hand": {"consume": {"mode": "shrink", "count": 4}}}
                """).hand().orElseThrow().consume();
        assertEquals(4, exact.count().resolve(1).min());
        assertEquals(4, exact.count().resolve(1).max());

        InteractionSpec.ConsumeSpec range = parse("""
                {"type": "right_click", "hand": {"consume": {"mode": "shrink", "chance": 0.33,
                    "count": {"min": 3, "max": 5}}}}
                """).hand().orElseThrow().consume();
        assertEquals(0.33, range.chance());
        assertEquals(3, range.count().resolve(1).min());
        assertEquals(5, range.count().resolve(1).max());
    }

    @Test
    @DisplayName("target defaults to a block with no state or NBT constraints")
    void targetDefaults() {
        InteractionSpec.TargetSpec target = parse("{\"type\": \"right_click\", \"target\": {}}").target().orElseThrow();

        assertEquals("block", target.kind());
        assertEquals(List.of(), target.match());
        assertTrue(target.state().whitelist().isEmpty());
        assertTrue(target.nbt().whitelist().isEmpty());
    }

    @Test
    @DisplayName("rewards default to one roll of an empty pool")
    void rewardsDefaults() {
        InteractionSpec.RewardsSpec rewards = parse("{\"type\": \"right_click\", \"rewards\": {}}").rewards().orElseThrow();

        assertEquals(1, rewards.rolls());
        assertTrue(rewards.weighted().isEmpty());
        assertTrue(rewards.guaranteed().isEmpty());
        assertTrue(rewards.fortune().isEmpty());
    }

    @Test
    @DisplayName("a reward entry defaults to weight 1 and count 1")
    void rewardEntryDefaults() {
        InteractionSpec.RewardsSpec rewards = parse("""
                {"type": "right_click", "rewards": {"weighted": [{"match": "minecraft:clay_ball"}]}}
                """).rewards().orElseThrow();

        InteractionSpec.RewardEntrySpec entry = rewards.weighted().get(0);
        assertEquals(1, entry.weight());
        assertEquals(new CountSpec.Range(1, 1), entry.count().resolve(0));
    }

    @Test
    @DisplayName("a reward entry requires a match")
    void rewardEntryRequiresMatch() {
        assertTrue(tryParse("{\"type\": \"right_click\", \"rewards\": {\"weighted\": [{\"weight\": 3}]}}").isError());
    }

    @Test
    @DisplayName("fortune defaults to vanilla fortune at factor 1")
    void fortuneDefaults() {
        InteractionSpec.FortuneSpec fortune = parse("{\"type\": \"right_click\", \"rewards\": {\"fortune\": {}}}")
                .rewards().orElseThrow().fortune().orElseThrow();

        assertEquals("minecraft:fortune", fortune.enchant());
        assertEquals(1.0, fortune.factor());
    }

    @Test
    @DisplayName("a transformation requires an explicit chance")
    void transformationRequiresChance() {
        assertTrue(tryParse("{\"type\": \"right_click\", \"transformation\": {}}").isError());
        assertEquals(0.5, parse("{\"type\": \"right_click\", \"transformation\": {\"chance\": 0.5}}")
                .transformation().orElseThrow().chance());
    }

    @Test
    @DisplayName("a cost requires an amount but not a chance")
    void costDefaults() {
        InteractionSpec.CostSpec damage = parse("{\"type\": \"right_click\", \"costs\": {\"damage\": {\"amount\": 2}}}")
                .costs().orElseThrow().damage().orElseThrow();

        assertEquals(1.0, damage.chance());
        assertEquals(new CountSpec.Range(2, 2), damage.amount().resolve(1));

        assertTrue(tryParse("{\"type\": \"right_click\", \"costs\": {\"damage\": {}}}").isError());
    }

    @Test
    @DisplayName("conditions default to matching everything")
    void conditionsDefaults() {
        InteractionSpec.ConditionsSpec conditions =
                parse("{\"type\": \"right_click\", \"conditions\": {}}").conditions().orElseThrow();

        assertEquals("any", conditions.time());
        assertTrue(conditions.weather().isEmpty());
        assertTrue(conditions.biomes().whitelist().isEmpty());
        assertTrue(conditions.yRange().isEmpty());
        assertTrue(conditions.requiresSneaking().isEmpty());
        assertEquals(0, conditions.playerState().minFood());
    }

    @Test
    @DisplayName("an effect defaults to 10 seconds at amplifier 0, always applied")
    void effectDefaults() {
        InteractionSpec.EffectSpec effect =
                parse("{\"type\": \"right_click\", \"effects\": [{\"id\": \"minecraft:haste\"}]}").effects().get(0);

        assertEquals(200, effect.duration());
        assertEquals(0, effect.amplifier());
        assertEquals(1.0, effect.chance());
    }

    @Test
    @DisplayName("a full file parses and every section arrives")
    void fullFile() {
        InteractionSpec spec = parse("""
                {
                  "schema_version": 2,
                  "enabled": true,
                  "hidden": true,
                  "type": "shift_right_click",
                  "hand": {
                    "hand": "main",
                    "match": ["minecraft:diamond_pickaxe"],
                    "consume": { "mode": "durability", "chance": 0.5 },
                    "nbt": { "whitelist": "{Damage:0}" },
                    "nbt_predicates": [{ "path": "Enchantments[].lvl", "int_range": [1, 5] }]
                  },
                  "target": {
                    "kind": "block",
                    "match": "#minecraft:logs",
                    "state": { "whitelist": { "axis": "y" } },
                    "nbt": { "blacklist": "{Locked:1b}" }
                  },
                  "transformation": {
                    "chance": 0.25,
                    "into": { "kind": "block", "id": "minecraft:air" },
                    "sound": "minecraft:block.wood.break"
                  },
                  "rewards": {
                    "weighted": [{ "match": "minecraft:stick", "weight": 9, "count": { "min": 1, "max": 3 } }],
                    "guaranteed": [{ "match": "minecraft:oak_sapling" }],
                    "rolls": 2,
                    "fortune": { "enchant": "minecraft:fortune", "factor": 0.5 }
                  },
                  "costs": { "hunger": { "chance": 0.5, "amount": { "min": 1, "max": 2 } } },
                  "conditions": {
                    "biomes": { "whitelist": ["#minecraft:is_forest"] },
                    "time": "night",
                    "weather": ["rain", "thunder"],
                    "y_range": [0, 128],
                    "light": { "max": 7 },
                    "requires_sneaking": true,
                    "player_state": { "min_food": 6, "min_xp_levels": 1 }
                  },
                  "effects": [{ "id": "minecraft:haste", "duration": 100, "amplifier": 1, "chance": 0.25 }],
                  "sound": "minecraft:entity.player.levelup",
                  "particles": "minecraft:dirt"
                }
                """);

        assertTrue(spec.hidden());
        assertEquals("shift_right_click", spec.type());
        assertEquals("durability", spec.hand().orElseThrow().consume().mode());
        assertEquals(1, spec.hand().orElseThrow().nbtPredicates().size());
        assertEquals(List.of("#minecraft:logs"), spec.target().orElseThrow().match());
        assertEquals("y", spec.target().orElseThrow().state().whitelist().get("axis"));
        assertEquals(2, spec.rewards().orElseThrow().rolls());
        assertEquals(1, spec.rewards().orElseThrow().guaranteed().size());
        assertEquals("night", spec.conditions().orElseThrow().time());
        assertEquals(List.of(0, 128), spec.conditions().orElseThrow().yRange().orElseThrow());
        assertEquals(1, spec.effects().size());
        assertEquals("minecraft:dirt", spec.particles().orElseThrow());
    }

    @Test
    @DisplayName("a parsed spec round-trips through its own codec")
    void roundTrip() {
        String json = """
                {
                  "type": "left_click",
                  "hidden": true,
                  "target": { "kind": "fluid", "match": "minecraft:water" },
                  "rewards": { "weighted": [{ "match": "minecraft:clay_ball", "weight": 4 }] }
                }
                """;

        InteractionSpec original = parse(json);
        JsonElement encoded = InteractionSpec.CODEC.encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow(message -> new AssertionError(message));

        assertEquals(original, parse(encoded.toString()));
    }

    @Test
    @DisplayName("equal files hash equally and different ones do not")
    void structuralEquality() {
        // PtaInteraction leans on this to tell "reloaded unchanged" from "edited", which is what
        // keeps the viewers from churning their whole category on every reload.
        String json = "{\"type\": \"left_click\", \"rewards\": {\"rolls\": 2}}";

        assertEquals(parse(json), parse(json));
        assertEquals(parse(json).hashCode(), parse(json).hashCode());
        assertNotEquals(parse(json), parse("{\"type\": \"left_click\", \"rewards\": {\"rolls\": 3}}"));
    }
}
