package com.drimoz.punchthemall.core.registry;

import com.drimoz.punchthemall.core.codec.InteractionSpec;
import com.drimoz.punchthemall.core.model.classes.PtaInteraction;
import com.drimoz.punchthemall.core.model.enums.PtaTypeEnum;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Loading, indexing and click-time filtering. The registry is a singleton, so each test resets it. */
class InteractionRegistryTest {

    private static final BlockPos POS = new BlockPos(0, 64, 0);

    private final InteractionRegistry registry = InteractionRegistry.getInstance();

    private final Level level = mock(Level.class);
    private final Player player = mock(Player.class);

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        registry.clearInteractions();

        BlockState stone = Blocks.STONE.defaultBlockState();
        FluidState empty = Fluids.EMPTY.defaultFluidState();

        lenient().when(level.getBlockState(POS)).thenReturn(stone);
        lenient().when(level.getFluidState(POS)).thenReturn(empty);
        lenient().when(level.dimension()).thenReturn(Level.OVERWORLD);

        Holder<Biome> biome = mock(Holder.class);
        lenient().when(biome.unwrapKey()).thenReturn(java.util.Optional.of(
                ResourceKey.create(Registries.BIOME, Identifier.parse("minecraft:plains"))));
        lenient().when(biome.is(org.mockito.ArgumentMatchers.<net.minecraft.tags.TagKey<Biome>>any())).thenReturn(false);
        lenient().when(level.getBiome(POS)).thenReturn(biome);

        lenient().when(player.isShiftKeyDown()).thenReturn(false);
        lenient().when(player.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(ItemStack.EMPTY);
        lenient().when(player.getItemInHand(InteractionHand.OFF_HAND)).thenReturn(ItemStack.EMPTY);
    }

    @AfterEach
    void tearDown() {
        registry.clearInteractions();
    }

    private static InteractionSpec spec(String json) {
        return InteractionSpec.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .getOrThrow(message -> new AssertionError(message));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("pta_test", path);
    }

    private void load(Map<String, String> files) {
        Map<Identifier, InteractionSpec> specs = new LinkedHashMap<>();
        files.forEach((path, json) -> specs.put(id(path), spec(json)));
        registry.rebuildFrom(specs, null);
    }

    private void loadOne(String path, String json) {
        load(new LinkedHashMap<>(Map.of(path, json)));
    }

    @Test
    @DisplayName("an empty load leaves an empty registry")
    void emptyLoad() {
        registry.rebuildFrom(Map.of(), null);

        assertTrue(registry.getInteractions().isEmpty());
        assertTrue(registry.getVisibleInteractions().isEmpty());
        assertEquals(0, registry.getJEIRowCount());
        assertFalse(registry.hasLeftClickAirInteraction());
    }

    @Test
    @DisplayName("schema_version below 2 is refused")
    void legacySchemaRefused() {
        loadOne("legacy", "{\"schema_version\": 1, \"type\": \"left_click\"}");
        assertTrue(registry.getInteractions().isEmpty());
    }

    @Test
    @DisplayName("enabled:false never loads")
    void disabledNotLoaded() {
        loadOne("off", "{\"type\": \"left_click\", \"enabled\": false}");
        assertTrue(registry.getInteractions().isEmpty());
    }

    @Test
    @DisplayName("hidden:true loads and can fire, but the viewers skip it")
    void hiddenLoadsButIsNotShown() {
        load(new LinkedHashMap<>(Map.of(
                "shown", "{\"type\": \"left_click\", \"target\": {\"match\": \"minecraft:stone\"}}",
                "secret", "{\"type\": \"left_click\", \"hidden\": true, \"target\": {\"match\": \"minecraft:stone\"}}")));

        assertEquals(2, registry.getInteractions().size());
        assertEquals(1, registry.getVisibleInteractions().size());
        assertEquals(id("shown"), registry.getVisibleInteractions().get(0).getId());

        // And it still matches a click — hidden is about display, not behaviour.
        assertEquals(2, registry.getFilteredInteractions(PtaTypeEnum.LEFT_CLICK, true, player, POS, level).size());
    }

    @Test
    @DisplayName("a spec that fails to resolve is skipped, and the rest still load")
    void badSpecDoesNotAbortTheLoad() {
        // The load runs inside the datapack reload, where an exception costs the pack every
        // interaction rather than the one file that is wrong.
        load(new LinkedHashMap<>(Map.of(
                "bad", "{\"type\": \"not_a_click\"}",
                "good", "{\"type\": \"left_click\", \"target\": {\"match\": \"minecraft:stone\"}}")));

        assertEquals(1, registry.getInteractions().size());
        assertNotNull(registry.getInteractionById(id("good")));
        assertNull(registry.getInteractionById(id("bad")));
    }

    @Test
    @DisplayName("interactions are stored and indexed in id order")
    void deterministicOrder() {
        // Candidate order decides which interactions survive max_matches_per_click and which one
        // transforms the block, so it must not depend on hash iteration order.
        Map<String, String> files = new LinkedHashMap<>();
        files.put("zulu", "{\"type\": \"left_click\", \"target\": {\"match\": \"minecraft:stone\"}}");
        files.put("alpha", "{\"type\": \"left_click\", \"target\": {\"match\": \"minecraft:stone\"}}");
        files.put("mike", "{\"type\": \"left_click\", \"target\": {\"match\": \"minecraft:stone\"}}");
        load(files);

        List<Identifier> order = registry.getInteractions().keySet().stream().toList();
        assertEquals(List.of(id("alpha"), id("mike"), id("zulu")), order);

        List<PtaInteraction> matches =
                registry.getFilteredInteractions(PtaTypeEnum.LEFT_CLICK, true, player, POS, level);
        assertEquals(List.of(id("alpha"), id("mike"), id("zulu")),
                matches.stream().map(PtaInteraction::getId).toList());
    }

    @Test
    @DisplayName("reloading the same files produces an equal set")
    void reloadIsStable() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("one", "{\"type\": \"left_click\", \"target\": {\"match\": \"minecraft:stone\"}}");

        load(files);
        List<PtaInteraction> first = List.copyOf(registry.getInteractions().values());
        load(files);
        List<PtaInteraction> second = List.copyOf(registry.getInteractions().values());

        assertEquals(first, second, "an unchanged reload must not look like a whole new recipe set");
    }

    @Test
    @DisplayName("an edited file produces a different set")
    void editedFileDiffers() {
        loadOne("one", "{\"type\": \"left_click\", \"target\": {\"match\": \"minecraft:stone\"}}");
        List<PtaInteraction> before = List.copyOf(registry.getInteractions().values());

        loadOne("one", "{\"type\": \"left_click\", \"target\": {\"match\": \"minecraft:dirt\"}}");
        List<PtaInteraction> after = List.copyOf(registry.getInteractions().values());

        assertFalse(before.equals(after));
    }

    @Test
    @DisplayName("clearing empties the registry and its indexes")
    void clearing() {
        loadOne("air", "{\"type\": \"left_click\"}");
        assertTrue(registry.hasLeftClickAirInteraction());

        registry.clearInteractions();

        assertTrue(registry.getInteractions().isEmpty());
        assertFalse(registry.hasLeftClickAirInteraction());
        assertTrue(registry.getFilteredInteractions(PtaTypeEnum.LEFT_CLICK, false, player, POS, level).isEmpty());
    }

    @Test
    @DisplayName("the left-click-air shortcut only fires for left-click air interactions")
    void leftClickAirDetection() {
        loadOne("right_air", "{\"type\": \"right_click\"}");
        assertFalse(registry.hasLeftClickAirInteraction());

        loadOne("left_block", "{\"type\": \"left_click\", \"target\": {\"match\": \"minecraft:stone\"}}");
        assertFalse(registry.hasLeftClickAirInteraction());

        loadOne("left_air", "{\"type\": \"left_click\"}");
        assertTrue(registry.hasLeftClickAirInteraction());

        loadOne("shift_left_air", "{\"type\": \"shift_left_click\"}");
        assertTrue(registry.hasLeftClickAirInteraction());
    }

    @Test
    @DisplayName("a click only matches its own type")
    void typeFilter() {
        loadOne("left", "{\"type\": \"left_click\", \"target\": {\"match\": \"minecraft:stone\"}}");

        assertEquals(1, registry.getFilteredInteractions(PtaTypeEnum.LEFT_CLICK, true, player, POS, level).size());
        assertEquals(0, registry.getFilteredInteractions(PtaTypeEnum.RIGHT_CLICK, true, player, POS, level).size());
    }

    @Test
    @DisplayName("sneaking selects the shift variant")
    void sneakSelectsShiftVariant() {
        load(new LinkedHashMap<>(Map.of(
                "plain", "{\"type\": \"left_click\", \"target\": {\"match\": \"minecraft:stone\"}}",
                "sneak", "{\"type\": \"shift_left_click\", \"target\": {\"match\": \"minecraft:stone\"}}")));

        when(player.isShiftKeyDown()).thenReturn(false);
        assertEquals(List.of(id("plain")),
                registry.getFilteredInteractions(PtaTypeEnum.LEFT_CLICK, true, player, POS, level)
                        .stream().map(PtaInteraction::getId).toList());

        when(player.isShiftKeyDown()).thenReturn(true);
        assertEquals(List.of(id("sneak")),
                registry.getFilteredInteractions(PtaTypeEnum.LEFT_CLICK, true, player, POS, level)
                        .stream().map(PtaInteraction::getId).toList());
    }

    @Test
    @DisplayName("a block interaction does not answer a click on nothing, and vice versa")
    void airVersusBlock() {
        load(new LinkedHashMap<>(Map.of(
                "block", "{\"type\": \"left_click\", \"target\": {\"match\": \"minecraft:stone\"}}",
                "air", "{\"type\": \"left_click\"}")));

        assertEquals(List.of(id("block")),
                registry.getFilteredInteractions(PtaTypeEnum.LEFT_CLICK, true, player, POS, level)
                        .stream().map(PtaInteraction::getId).toList());
        assertEquals(List.of(id("air")),
                registry.getFilteredInteractions(PtaTypeEnum.LEFT_CLICK, false, player, POS, level)
                        .stream().map(PtaInteraction::getId).toList());
    }

    @Test
    @DisplayName("a different block does not match")
    void wrongBlockDoesNotMatch() {
        loadOne("dirt", "{\"type\": \"left_click\", \"target\": {\"match\": \"minecraft:dirt\"}}");

        assertTrue(registry.getFilteredInteractions(PtaTypeEnum.LEFT_CLICK, true, player, POS, level).isEmpty());
    }

    @Test
    @DisplayName("an empty hand requirement needs both hands empty")
    void emptyHandFilter() {
        loadOne("bare", """
                {"type": "left_click", "target": {"match": "minecraft:stone"}, "hand": {"hand": "any"}}
                """);

        assertEquals(1, registry.getFilteredInteractions(PtaTypeEnum.LEFT_CLICK, true, player, POS, level).size());

        when(player.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(new ItemStack(Items.STICK));
        assertTrue(registry.getFilteredInteractions(PtaTypeEnum.LEFT_CLICK, true, player, POS, level).isEmpty());
    }

    @Test
    @DisplayName("a hand item requirement matches the named item in the named hand")
    void handItemFilter() {
        loadOne("stick", """
                {"type": "left_click", "target": {"match": "minecraft:stone"},
                 "hand": {"hand": "main", "match": "minecraft:stick"}}
                """);

        assertTrue(registry.getFilteredInteractions(PtaTypeEnum.LEFT_CLICK, true, player, POS, level).isEmpty());

        when(player.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(new ItemStack(Items.STICK));
        assertEquals(1, registry.getFilteredInteractions(PtaTypeEnum.LEFT_CLICK, true, player, POS, level).size());

        // The same item in the off hand does not satisfy a main-hand requirement.
        when(player.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(ItemStack.EMPTY);
        when(player.getItemInHand(InteractionHand.OFF_HAND)).thenReturn(new ItemStack(Items.STICK));
        assertTrue(registry.getFilteredInteractions(PtaTypeEnum.LEFT_CLICK, true, player, POS, level).isEmpty());
    }

    @Test
    @DisplayName("`any` hand accepts either side")
    void anyHandFilter() {
        loadOne("stick", """
                {"type": "left_click", "target": {"match": "minecraft:stone"},
                 "hand": {"hand": "any", "match": "minecraft:stick"}}
                """);

        when(player.getItemInHand(InteractionHand.OFF_HAND)).thenReturn(new ItemStack(Items.STICK));
        assertEquals(1, registry.getFilteredInteractions(PtaTypeEnum.LEFT_CLICK, true, player, POS, level).size());
    }

    @Test
    @DisplayName("a dimension whitelist gates on the current dimension")
    void dimensionWhitelist() {
        loadOne("nether_only", """
                {"type": "left_click", "target": {"match": "minecraft:stone"},
                 "conditions": {"biomes": {"whitelist": ["minecraft:the_nether"]}}}
                """);
        assertTrue(registry.getFilteredInteractions(PtaTypeEnum.LEFT_CLICK, true, player, POS, level).isEmpty());

        loadOne("overworld_only", """
                {"type": "left_click", "target": {"match": "minecraft:stone"},
                 "conditions": {"biomes": {"whitelist": ["minecraft:overworld"]}}}
                """);
        assertEquals(1, registry.getFilteredInteractions(PtaTypeEnum.LEFT_CLICK, true, player, POS, level).size());
    }

    @Test
    @DisplayName("a biome blacklist excludes the current biome")
    void biomeBlacklist() {
        loadOne("not_plains", """
                {"type": "left_click", "target": {"match": "minecraft:stone"},
                 "conditions": {"biomes": {"blacklist": ["minecraft:plains"]}}}
                """);
        assertTrue(registry.getFilteredInteractions(PtaTypeEnum.LEFT_CLICK, true, player, POS, level).isEmpty());

        loadOne("not_desert", """
                {"type": "left_click", "target": {"match": "minecraft:stone"},
                 "conditions": {"biomes": {"blacklist": ["minecraft:desert"]}}}
                """);
        assertEquals(1, registry.getFilteredInteractions(PtaTypeEnum.LEFT_CLICK, true, player, POS, level).size());
    }

    @Test
    @DisplayName("a malformed biome tag is skipped instead of throwing on every click")
    void malformedBiomeTagDoesNotThrow() {
        loadOne("bad_tag", """
                {"type": "left_click", "target": {"match": "minecraft:stone"},
                 "conditions": {"biomes": {"whitelist": ["#NOT A TAG"]}}}
                """);

        assertDoesNotThrow(() ->
                registry.getFilteredInteractions(PtaTypeEnum.LEFT_CLICK, true, player, POS, level));
        assertTrue(registry.getFilteredInteractions(PtaTypeEnum.LEFT_CLICK, true, player, POS, level).isEmpty());
    }

    @Test
    @DisplayName("the tallest visible drop grid drives the category height")
    void jeiRowCountIgnoresHidden() {
        StringBuilder tenDrops = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            tenDrops.append(i > 0 ? "," : "").append("{\"match\": \"minecraft:stick\", \"count\": ").append(i + 1).append("}");
        }

        loadOne("hidden_big", "{\"type\": \"left_click\", \"hidden\": true, \"rewards\": {\"weighted\": ["
                + tenDrops + "]}}");
        assertEquals(0, registry.getJEIRowCount(), "a hidden recipe must not stretch the category");

        loadOne("shown_big", "{\"type\": \"left_click\", \"rewards\": {\"weighted\": [" + tenDrops + "]}}");
        assertEquals(2, registry.getJEIRowCount());
    }

    @Test
    @DisplayName("the exposed maps are unmodifiable")
    void snapshotIsImmutable() {
        // The snapshot is shared between the server thread and the client thread; handing out a
        // mutable view of it would defeat the point of publishing it in one write.
        loadOne("one", "{\"type\": \"left_click\"}");

        assertThrowsUnsupported(() -> registry.getInteractions().clear());
        assertThrowsUnsupported(() -> registry.getVisibleInteractions().clear());
    }

    private static void assertThrowsUnsupported(Runnable action) {
        try {
            action.run();
            throw new AssertionError("expected the collection to be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // exactly right
        }
    }
}
