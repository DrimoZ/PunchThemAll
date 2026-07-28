package com.drimoz.punchthemall.core.model.records;

import com.drimoz.punchthemall.McBootstrap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** One drop entry: its count range, its emptiness rule, and the NBT it carries. */
class PtaDropRecordTest {

    @BeforeAll
    static void bootstrap() {
        McBootstrap.ensure();
    }

    private static Set<Item> items(Item... items) {
        return new HashSet<>(Set.of(items));
    }

    private static RandomSource seeded() {
        return RandomSource.create(1234L);
    }

    @Test
    @DisplayName("min 0 with a positive max is a real 'nothing to n' roll, not an empty entry")
    void zeroMinIsNotEmpty() {
        // Regression: isEmpty() keyed on min, and the constructor forced max to 0 whenever min was 0.
        // So {"min": 0, "max": 3} — which the schema and docs both advertise — silently never
        // dropped anything and vanished from the viewers, while still consuming its pool weight.
        PtaDropRecord record = new PtaDropRecord(items(Items.DIAMOND), 0, 3, null);

        assertFalse(record.isEmpty());
        assertEquals(0, record.min());
        assertEquals(3, record.max());
    }

    @Test
    @DisplayName("a zero max is genuinely empty")
    void zeroMaxIsEmpty() {
        assertTrue(new PtaDropRecord(items(Items.DIAMOND), 0, 0, null).isEmpty());
    }

    @Test
    @DisplayName("no items, or only air, is empty")
    void emptyByItems() {
        assertTrue(new PtaDropRecord(items(), 1, 1, null).isEmpty());
        assertTrue(new PtaDropRecord(items(Items.AIR), 1, 1, null).isEmpty());
        assertFalse(new PtaDropRecord(items(Items.AIR, Items.DIAMOND), 1, 1, null).isEmpty());
    }

    @Test
    @DisplayName("a negative min is floored and max never sits below it")
    void boundsAreNormalised() {
        PtaDropRecord record = new PtaDropRecord(items(Items.DIAMOND), -4, -1, null);
        assertEquals(0, record.min());
        assertEquals(0, record.max());

        PtaDropRecord inverted = new PtaDropRecord(items(Items.DIAMOND), 5, 2, null);
        assertEquals(5, inverted.min());
        assertEquals(5, inverted.max());
    }

    @Test
    @DisplayName("an exact count always produces that many")
    void exactCount() {
        PtaDropRecord record = new PtaDropRecord(items(Items.DIAMOND), 4, 4, null);
        RandomSource random = seeded();

        for (int i = 0; i < 50; i++) {
            assertEquals(4, record.calculateCount(random));
        }
    }

    @Test
    @DisplayName("a range stays inside its bounds and reaches both of them")
    void rangeCoversItsBounds() {
        PtaDropRecord record = new PtaDropRecord(items(Items.DIAMOND), 1, 3, null);
        RandomSource random = seeded();

        boolean sawMin = false;
        boolean sawMax = false;
        for (int i = 0; i < 500; i++) {
            int count = record.calculateCount(random);
            assertTrue(count >= 1 && count <= 3, "count out of range: " + count);
            sawMin |= count == 1;
            sawMax |= count == 3;
        }

        assertTrue(sawMin, "never rolled the minimum");
        assertTrue(sawMax, "never rolled the maximum");
    }

    @Test
    @DisplayName("a [0, n] roll of zero yields EMPTY rather than a zero-count stack")
    void zeroRollYieldsEmptyStack() {
        PtaDropRecord record = new PtaDropRecord(items(Items.DIAMOND), 0, 1, null);
        RandomSource random = seeded();

        boolean sawEmpty = false;
        for (int i = 0; i < 200; i++) {
            ItemStack stack = record.getItemStack(random);
            if (stack.isEmpty()) {
                sawEmpty = true;
                assertSame(ItemStack.EMPTY, stack);
            } else {
                assertEquals(1, stack.getCount());
            }
        }

        assertTrue(sawEmpty, "a [0,1] range never rolled zero");
    }

    @Test
    @DisplayName("an empty entry yields EMPTY")
    void emptyEntryYieldsEmptyStack() {
        assertSame(ItemStack.EMPTY, new PtaDropRecord(items(), 1, 1, null).getItemStack(seeded()));
    }

    @Test
    @DisplayName("the picked item always comes from the entry's set")
    void picksFromItsOwnSet() {
        Set<Item> allowed = items(Items.DIAMOND, Items.EMERALD, Items.GOLD_INGOT);
        PtaDropRecord record = new PtaDropRecord(allowed, 1, 1, null);
        RandomSource random = seeded();

        for (int i = 0; i < 200; i++) {
            assertTrue(allowed.contains(record.pickRandomItem(random)));
        }
    }

    @Test
    @DisplayName("authored NBT is applied to the produced stack")
    void nbtIsApplied() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("Damage", 7);

        ItemStack stack = new PtaDropRecord(items(Items.DIAMOND_PICKAXE), 1, 1, nbt).getItemStack(seeded());

        assertFalse(stack.isEmpty());
        assertEquals(7, stack.getDamageValue());
    }

    @Test
    @DisplayName("a null NBT becomes an empty compound rather than staying null")
    void nullNbtNormalised() {
        assertNotNull(new PtaDropRecord(items(Items.DIAMOND), 1, 1, null).nbt());
        assertTrue(new PtaDropRecord(items(Items.DIAMOND), 1, 1, null).nbt().isEmpty());
    }

    @Test
    @DisplayName("NBT survives on a zero-floored entry")
    void nbtKeptWhenMinIsZero() {
        // The old constructor dropped the NBT whenever min was 0, together with the max.
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("Damage", 3);

        assertFalse(new PtaDropRecord(items(Items.DIAMOND_PICKAXE), 0, 2, nbt).nbt().isEmpty());
    }
}
