package com.drimoz.punchthemall.core.checker;

import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Registry lookups from authored strings.
 *
 * <p>The important property is that a malformed id is ordinary input, not an exception. These
 * lookups run inside the datapack reload and, for biome tags, on every click — {@code
 * ResourceLocation.parse} throwing meant one typo could abort a pack's whole load.</p>
 *
 * <p>Tag lookups return empty here because tags need a running server; that is the same reason
 * production resolves them on {@code TagsUpdatedEvent} rather than at parse time.</p>
 */
class CheckersTest {

    /** Ids that {@code ResourceLocation} refuses: spaces, capitals, punctuation, empty namespace. */
    private static final String[] MALFORMED = {
            "NOT AN ID", "Upper:Case", "a b c", "???", "minecraft:bad path", "минкрафт:стоун", ":", ""
    };

    @Test
    @DisplayName("known items, blocks and fluids resolve")
    void knownIdsResolve() {
        assertTrue(ItemChecker.doesItemExist("minecraft:stick"));
        assertEquals(Items.STICK, ItemChecker.getExistingItem("minecraft:stick"));

        assertTrue(BlockChecker.doesBlockExist("minecraft:stone"));
        assertEquals(Blocks.STONE, BlockChecker.getExistingBlock("minecraft:stone"));

        assertTrue(FluidChecker.doesFluidExist("minecraft:water"));
        assertEquals(Fluids.WATER, FluidChecker.getExistingFluid("minecraft:water"));
    }

    @Test
    @DisplayName("an id without a namespace defaults to minecraft")
    void bareIdDefaultsToMinecraft() {
        assertTrue(ItemChecker.doesItemExist("stick"));
        assertEquals(Items.STICK, ItemChecker.getExistingItem("stick"));
    }

    @Test
    @DisplayName("a well-formed but unregistered id simply does not exist")
    void unknownIdsAreAbsent() {
        assertFalse(ItemChecker.doesItemExist("minecraft:not_an_item"));
        assertFalse(BlockChecker.doesBlockExist("somemod:not_a_block"));
        assertFalse(FluidChecker.doesFluidExist("minecraft:not_a_fluid"));
    }

    @Test
    @DisplayName("a malformed id never throws")
    void malformedIdsDoNotThrow() {
        for (String id : MALFORMED) {
            assertDoesNotThrow(() -> ItemChecker.doesItemExist(id), id);
            assertDoesNotThrow(() -> BlockChecker.doesBlockExist(id), id);
            assertDoesNotThrow(() -> FluidChecker.doesFluidExist(id), id);
            assertDoesNotThrow(() -> ItemChecker.getItemsForTag(id), id);
            assertDoesNotThrow(() -> BlockChecker.getBlocksForTag(id), id);
            assertDoesNotThrow(() -> FluidChecker.getFluidsForTag(id), id);
            assertDoesNotThrow(() -> ItemChecker.isItemTagExisting(id), id);
            assertDoesNotThrow(() -> BlockChecker.isBlockTagExisting(id), id);
            assertDoesNotThrow(() -> FluidChecker.isFluidTagExisting(id), id);
        }
    }

    @Test
    @DisplayName("a malformed id reports as absent, and getters return null rather than a default")
    void malformedIdsReportAbsent() {
        for (String id : MALFORMED) {
            assertFalse(ItemChecker.doesItemExist(id), id);
            assertFalse(BlockChecker.doesBlockExist(id), id);
            assertFalse(FluidChecker.doesFluidExist(id), id);

            assertNull(ItemChecker.getExistingItem(id), id);
            assertNull(BlockChecker.getExistingBlock(id), id);
            assertNull(FluidChecker.getExistingFluid(id), id);

            assertTrue(ItemChecker.getItemsForTag(id).isEmpty(), id);
            assertTrue(BlockChecker.getBlocksForTag(id).isEmpty(), id);
            assertTrue(FluidChecker.getFluidsForTag(id).isEmpty(), id);

            assertFalse(ItemChecker.isItemTagExisting(id), id);
            assertFalse(BlockChecker.isBlockTagExisting(id), id);
            assertFalse(FluidChecker.isFluidTagExisting(id), id);
        }
    }

    @Test
    @DisplayName("a null id is treated as absent")
    void nullIsAbsent() {
        assertFalse(ItemChecker.doesItemExist(null));
        assertNull(ItemChecker.getExistingItem(null));
        assertTrue(BlockChecker.getBlocksForTag(null).isEmpty());
        assertFalse(FluidChecker.isFluidTagExisting(null));
    }

    @Test
    @DisplayName("tags resolve to nothing outside a running server, without failing")
    void tagsAreEmptyWithoutAServer() {
        assertTrue(ItemChecker.getItemsForTag("minecraft:planks").isEmpty());
        assertNull(ItemChecker.getFirstItemFromTag("minecraft:planks"));
        assertNull(BlockChecker.getFirstBlockForTag("minecraft:logs"));
        assertNull(FluidChecker.getFirstFluidForTag("minecraft:water"));
    }
}
