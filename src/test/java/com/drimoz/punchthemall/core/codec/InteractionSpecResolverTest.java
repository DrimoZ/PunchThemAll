package com.drimoz.punchthemall.core.codec;

import com.drimoz.punchthemall.core.model.classes.PtaConditions;
import com.drimoz.punchthemall.core.model.classes.PtaInteraction;
import com.drimoz.punchthemall.core.model.enums.PtaHandEnum;
import com.drimoz.punchthemall.core.model.enums.PtaTypeEnum;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Turning a parsed spec into the runtime model.
 *
 * <p>Tags are not available outside a running server, so everything here matches by id. That is not
 * a gap in coverage so much as the reason production resolves on {@code TagsUpdatedEvent}: a tag
 * resolved any earlier would be empty, which is exactly what happens in these tests.</p>
 *
 * <p>{@code registries} is null throughout, standing in for "the dynamic registries are not
 * reachable". Only the Fortune enchantment needs them, and it must degrade rather than throw.</p>
 */
class InteractionSpecResolverTest {

    private static final Identifier ID = Identifier.fromNamespaceAndPath("pta_test", "example");

    private static PtaInteraction resolve(String json) {
        InteractionSpec spec = InteractionSpec.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .getOrThrow(message -> new AssertionError(message));
        return InteractionSpecResolver.resolve(ID, spec, null);
    }

    @Test
    @DisplayName("a minimal file resolves to an air interaction with an empty hand")
    void minimal() {
        PtaInteraction interaction = resolve("{\"type\": \"right_click\"}");

        assertNotNull(interaction);
        assertEquals(ID, interaction.getId());
        assertEquals(PtaTypeEnum.RIGHT_CLICK, interaction.getType());
        assertTrue(interaction.getBlock().isAir());
        assertTrue(interaction.getHand().isEmpty());
        assertFalse(interaction.isHidden());
        assertFalse(interaction.hasHurtPlayer());
        assertFalse(interaction.hasConsumeFood());
    }

    @Test
    @DisplayName("an unknown click type is rejected outright")
    void unknownType() {
        assertNull(resolve("{\"type\": \"middle_click\"}"));
    }

    @Test
    @DisplayName("the hidden flag reaches the runtime model")
    void hiddenFlag() {
        assertTrue(resolve("{\"type\": \"right_click\", \"hidden\": true}").isHidden());
        assertFalse(resolve("{\"type\": \"right_click\", \"hidden\": false}").isHidden());
    }

    @Test
    @DisplayName("a hand resolves its items, hand side and consume mode")
    void hand() {
        PtaInteraction interaction = resolve("""
                {"type": "right_click", "hand": {
                   "hand": "off", "match": "minecraft:diamond_pickaxe",
                   "consume": { "mode": "durability", "chance": 0.5 }}}
                """);

        assertEquals(PtaHandEnum.OFF_HAND, interaction.getHand().getHand());
        assertTrue(interaction.getHand().getItemSet().contains(Items.DIAMOND_PICKAXE));
        assertTrue(interaction.getHand().isDamageable());
        assertFalse(interaction.getHand().isConsumable());
        assertEquals(0.5, interaction.getHand().getChance());
    }

    @Test
    @DisplayName("consume mode `shrink` and `consume` both mean shrink")
    void consumeAliases() {
        String template = "{\"type\": \"right_click\", \"hand\": {\"match\": \"minecraft:stick\", \"consume\": {\"mode\": \"%s\"}}}";

        assertTrue(resolve(template.formatted("shrink")).getHand().isConsumable());
        assertTrue(resolve(template.formatted("consume")).getHand().isConsumable());
        assertFalse(resolve(template.formatted("none")).getHand().isConsumable());
    }

    @Test
    @DisplayName("an unknown hand falls back to any hand rather than dropping the interaction")
    void unknownHand() {
        PtaInteraction interaction = resolve("{\"type\": \"right_click\", \"hand\": {\"hand\": \"third\"}}");

        assertNotNull(interaction);
        assertTrue(interaction.getHand().isEmpty());
    }

    @Test
    @DisplayName("a block target resolves by id")
    void blockTarget() {
        PtaInteraction interaction = resolve(
                "{\"type\": \"left_click\", \"target\": {\"kind\": \"block\", \"match\": \"minecraft:stone\"}}");

        assertTrue(interaction.getBlock().isBlock());
        assertTrue(interaction.getBlock().isBlockFromSet(Blocks.STONE));
    }

    @Test
    @DisplayName("a fluid target resolves by id")
    void fluidTarget() {
        PtaInteraction interaction = resolve(
                "{\"type\": \"left_click\", \"target\": {\"kind\": \"fluid\", \"match\": \"minecraft:water\"}}");

        assertTrue(interaction.getBlock().isFluid());
        assertEquals(Fluids.WATER, interaction.getBlock().getFluid());
    }

    @Test
    @DisplayName("kind `any` finds a block or a fluid without reporting the side that missed")
    void anyKind() {
        assertTrue(resolve("{\"type\": \"left_click\", \"target\": {\"kind\": \"any\", \"match\": \"minecraft:stone\"}}")
                .getBlock().isBlock());
        // Only registered as a fluid, so `any` has one place to find it.
        assertTrue(resolve("{\"type\": \"left_click\", \"target\": {\"kind\": \"any\", \"match\": \"minecraft:flowing_water\"}}")
                .getBlock().isFluid());
    }

    @Test
    @DisplayName("kind `any` on water or lava resolves to the block, since the id exists in both registries")
    void anyKindPrefersBlockForSharedIds() {
        // `minecraft:water` names both a block and a fluid, so `any` finds both and the mixed-target
        // rule keeps the blocks. Harmless in practice — a water source *is* Blocks.WATER at that
        // position, so the block index matches the click — but it means `kind: "fluid"` is the only
        // way to get a genuine fluid target, and the docs say so.
        assertTrue(resolve("{\"type\": \"left_click\", \"target\": {\"kind\": \"any\", \"match\": \"minecraft:water\"}}")
                .getBlock().isBlock());
        assertTrue(resolve("{\"type\": \"left_click\", \"target\": {\"kind\": \"fluid\", \"match\": \"minecraft:water\"}}")
                .getBlock().isFluid());
    }

    @Test
    @DisplayName("a target that resolves to nothing falls back to air")
    void unresolvableTargetBecomesAir() {
        assertTrue(resolve("{\"type\": \"left_click\", \"target\": {\"match\": \"minecraft:not_a_block\"}}")
                .getBlock().isAir());
    }

    @Test
    @DisplayName("a target mixing blocks and fluids keeps the blocks")
    void mixedTargetPrefersBlocks() {
        PtaInteraction interaction = resolve("""
                {"type": "left_click", "target": {"kind": "any", "match": ["minecraft:stone", "minecraft:water"]}}
                """);

        assertTrue(interaction.getBlock().isBlock());
        assertTrue(interaction.getBlock().isBlockFromSet(Blocks.STONE));
    }

    @Test
    @DisplayName("block states resolve against the target's properties")
    void blockState() {
        PtaInteraction interaction = resolve("""
                {"type": "left_click", "target": {
                   "match": "minecraft:oak_log", "state": { "whitelist": { "axis": "y" } }}}
                """);

        assertTrue(interaction.getBlock().hasStateWhiteList());
        assertEquals(1, interaction.getBlock().getStateWhiteList().size());
    }

    @Test
    @DisplayName("an unknown state property is dropped, not fatal")
    void unknownStateIgnored() {
        PtaInteraction interaction = resolve("""
                {"type": "left_click", "target": {
                   "match": "minecraft:stone", "state": { "whitelist": { "nonsense": "1" } }}}
                """);

        assertNotNull(interaction);
        assertFalse(interaction.getBlock().hasStateWhiteList());
    }

    @Test
    @DisplayName("rewards resolve, and a non-positive weight is dropped with the entry")
    void rewards() {
        PtaInteraction interaction = resolve("""
                {"type": "left_click", "rewards": {
                   "weighted": [
                     { "match": "minecraft:clay_ball", "weight": 4 },
                     { "match": "minecraft:diamond",  "weight": 0 }
                   ],
                   "guaranteed": [{ "match": "minecraft:stick", "count": 2 }],
                   "rolls": 3 }}
                """);

        assertEquals(4, interaction.getPool().getTotalPoolWeight());
        assertEquals(1, interaction.getPool().getTotalPoolSize());
        assertEquals(3, interaction.getRewards().getRolls());
        assertTrue(interaction.getRewards().hasGuaranteed());
    }

    @Test
    @DisplayName("a [0, n] drop survives resolution")
    void zeroMinDropSurvives() {
        PtaInteraction interaction = resolve("""
                {"type": "left_click", "rewards": {
                   "weighted": [{ "match": "minecraft:clay_ball", "count": { "min": 0, "max": 3 } }]}}
                """);

        assertEquals(1, interaction.getPool().getTotalPoolSize());
        assertEquals(0, interaction.getPool().getDropPool().keySet().iterator().next().min());
        assertEquals(3, interaction.getPool().getDropPool().keySet().iterator().next().max());
    }

    @Test
    @DisplayName("fortune degrades to no bonus when the enchantment registry is out of reach")
    void fortuneWithoutRegistries() {
        PtaInteraction interaction = resolve("""
                {"type": "left_click", "rewards": { "fortune": { "enchant": "minecraft:fortune", "factor": 2 }}}
                """);

        assertNotNull(interaction);
        assertFalse(interaction.getRewards().hasFortune());
    }

    @Test
    @DisplayName("costs resolve with a floor of one point")
    void costs() {
        PtaInteraction interaction = resolve("""
                {"type": "left_click", "costs": {
                   "damage": { "chance": 0.5, "amount": 2 },
                   "hunger": { "amount": { "min": 0, "max": 4 } }}}
                """);

        assertTrue(interaction.hasHurtPlayer());
        assertEquals(0.5, interaction.getHurtPlayer().chance());
        assertEquals(2, interaction.getHurtPlayer().min());

        assertTrue(interaction.hasConsumeFood());
        assertEquals(1, interaction.getConsumeFood().min(), "a cost floors at one point");
        assertEquals(4, interaction.getConsumeFood().max());
    }

    @Test
    @DisplayName("biome whitelist and blacklist land on the interaction")
    void biomes() {
        PtaInteraction whitelisted = resolve("""
                {"type": "left_click", "conditions": { "biomes": { "whitelist": ["minecraft:the_nether"] }}}
                """);
        assertTrue(whitelisted.hasBiomeWhiteList());
        assertFalse(whitelisted.hasBiomeBlackList());

        PtaInteraction blacklisted = resolve("""
                {"type": "left_click", "conditions": { "biomes": { "blacklist": ["minecraft:the_end"] }}}
                """);
        assertTrue(blacklisted.hasBiomeBlackList());
        assertFalse(blacklisted.hasBiomeWhiteList());
    }

    @Test
    @DisplayName("conditions resolve into the extras bundle")
    void conditions() {
        PtaInteraction interaction = resolve("""
                {"type": "left_click", "conditions": {
                   "time": "night", "weather": ["rain"], "y_range": [0, 60],
                   "light": { "max": 7 }, "player_state": { "min_food": 6 }}}
                """);

        PtaConditions conditions = interaction.getConditions();
        assertEquals(PtaConditions.Time.NIGHT, conditions.time());
        assertTrue(conditions.weather().contains(PtaConditions.Weather.RAIN));
        assertEquals(0, conditions.yMin());
        assertEquals(60, conditions.yMax());
        assertEquals(7, conditions.lightMax());
        assertEquals(6, conditions.minFood());
    }

    @Test
    @DisplayName("unknown time and weather tokens fall back to 'any' rather than failing")
    void unknownConditionTokens() {
        PtaInteraction interaction = resolve("""
                {"type": "left_click", "conditions": { "time": "dusk", "weather": ["hail"] }}
                """);

        assertEquals(PtaConditions.Time.ANY, interaction.getConditions().time());
        assertTrue(interaction.getConditions().weather().isEmpty());
    }

    @Test
    @DisplayName("effects resolve from the built-in registry")
    void effects() {
        PtaInteraction interaction = resolve("""
                {"type": "left_click", "effects": [{ "id": "minecraft:haste", "duration": 100, "amplifier": 1 }]}
                """);

        assertTrue(interaction.getExtras().hasEffects());
        assertEquals(100, interaction.getExtras().effects().get(0).duration());
        assertEquals(1, interaction.getExtras().effects().get(0).amplifier());
    }

    @Test
    @DisplayName("an unknown effect is skipped, keeping the rest of the interaction")
    void unknownEffectSkipped() {
        PtaInteraction interaction = resolve("""
                {"type": "left_click", "effects": [
                   { "id": "minecraft:not_an_effect" }, { "id": "minecraft:haste" }]}
                """);

        assertEquals(1, interaction.getExtras().effects().size());
    }

    @Test
    @DisplayName("a transformation with chance 0 is no transformation")
    void inertTransformation() {
        assertFalse(resolve("{\"type\": \"left_click\", \"target\": {\"match\": \"minecraft:stone\"}, "
                + "\"transformation\": {\"chance\": 0}}").getTransformation().hasTransformation());
    }

    @Test
    @DisplayName("a transformation into a block resolves, and an unknown one degrades to breaking")
    void transformationInto() {
        PtaInteraction into = resolve("""
                {"type": "left_click", "target": {"match": "minecraft:stone"},
                 "transformation": {"chance": 1, "into": {"kind": "block", "id": "minecraft:cobblestone"}}}
                """);
        assertTrue(into.getTransformation().isBlock());
        assertEquals(Blocks.COBBLESTONE, into.getTransformation().getBlock());

        PtaInteraction unknown = resolve("""
                {"type": "left_click", "target": {"match": "minecraft:stone"},
                 "transformation": {"chance": 1, "into": {"kind": "block", "id": "minecraft:not_a_block"}}}
                """);
        assertTrue(unknown.getTransformation().isAir());
    }

    @Test
    @DisplayName("an air target cannot carry a transformation")
    void airTargetDropsTransformation() {
        PtaInteraction interaction = resolve("""
                {"type": "left_click", "transformation": {"chance": 1, "into": {"id": "minecraft:stone"}}}
                """);

        assertTrue(interaction.getBlock().isAir());
        assertFalse(interaction.getTransformation().hasTransformation());
    }

    @Test
    @DisplayName("sound and particles resolve, and unknown ones simply do not appear")
    void feedback() {
        PtaInteraction known = resolve("""
                {"type": "left_click", "sound": "minecraft:entity.player.levelup", "particles": "minecraft:dirt"}
                """);
        assertTrue(known.getExtras().hasSound());
        assertTrue(known.getExtras().hasParticles());

        PtaInteraction unknown = resolve("""
                {"type": "left_click", "sound": "minecraft:no.such.sound", "particles": "minecraft:not_a_block"}
                """);
        assertFalse(unknown.getExtras().hasSound());
        assertFalse(unknown.getExtras().hasParticles());
    }

    @Test
    @DisplayName("a malformed id is reported, never thrown")
    void malformedIdsDoNotThrow() {
        // Every id in a file is authored text. Identifier.parse throws, and this runs inside
        // the datapack reload — one typo used to abort the load of every interaction in the pack.
        assertDoesNotThrow(() -> resolve("{\"type\": \"left_click\", \"sound\": \"NOT AN ID\"}"));
        assertDoesNotThrow(() -> resolve("{\"type\": \"left_click\", \"particles\": \"Bad Id!\"}"));
        assertDoesNotThrow(() -> resolve("{\"type\": \"left_click\", \"effects\": [{\"id\": \"UPPER CASE\"}]}"));
        assertDoesNotThrow(() -> resolve("{\"type\": \"left_click\", \"target\": {\"match\": \"a b c\"}}"));
        assertDoesNotThrow(() -> resolve("{\"type\": \"left_click\", \"hand\": {\"match\": \"???\"}}"));
        assertDoesNotThrow(() -> resolve(
                "{\"type\": \"left_click\", \"rewards\": {\"weighted\": [{\"match\": \"! !\"}]}}"));
        assertDoesNotThrow(() -> resolve(
                "{\"type\": \"left_click\", \"rewards\": {\"fortune\": {\"enchant\": \"nope!\"}}}"));
    }

    @Test
    @DisplayName("a malformed id yields the same inert result as an unknown one")
    void malformedIdsDegradeGracefully() {
        PtaInteraction interaction = resolve("""
                {"type": "left_click", "sound": "NOT AN ID", "target": {"match": "a b c"}}
                """);

        assertNotNull(interaction);
        assertFalse(interaction.getExtras().hasSound());
        assertTrue(interaction.getBlock().isAir());
    }

    @Test
    @DisplayName("two resolutions of the same file compare equal")
    void resolutionIsStable() {
        String json = "{\"type\": \"left_click\", \"rewards\": {\"rolls\": 2}}";

        assertEquals(resolve(json), resolve(json));
        assertEquals(resolve(json).hashCode(), resolve(json).hashCode());
    }
}
