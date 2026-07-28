package com.drimoz.punchthemall.core.model.classes;

import com.drimoz.punchthemall.McBootstrap;
import com.drimoz.punchthemall.core.model.records.PtaDropRecord;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Weighted picking, and the slot count the recipe viewers size their grid from. */
class PtaPoolTest {

    @BeforeAll
    static void bootstrap() {
        McBootstrap.ensure();
    }

    private static Set<Item> items(Item... items) {
        return new HashSet<>(Set.of(items));
    }

    private static PtaDropRecord one(Item item) {
        return new PtaDropRecord(items(item), 1, 1, null);
    }

    /** Insertion-ordered so a roll value maps to a predictable entry; production uses a HashMap. */
    private static PtaPool pool(Map<PtaDropRecord, Integer> entries) {
        return PtaPool.create(new LinkedHashMap<>(entries));
    }

    @Test
    @DisplayName("an absent or zero-weight pool is empty")
    void emptiness() {
        assertTrue(PtaPool.create(null).isEmpty());
        assertTrue(PtaPool.create(new LinkedHashMap<>()).isEmpty());
        assertTrue(pool(Map.of(one(Items.DIAMOND), 0)).isEmpty());
        assertFalse(pool(Map.of(one(Items.DIAMOND), 1)).isEmpty());
    }

    @Test
    @DisplayName("total weight is the sum of the entry weights")
    void totalWeight() {
        LinkedHashMap<PtaDropRecord, Integer> entries = new LinkedHashMap<>();
        entries.put(one(Items.DIAMOND), 10);
        entries.put(one(Items.EMERALD), 90);

        assertEquals(100, PtaPool.create(entries).getTotalPoolWeight());
    }

    @Test
    @DisplayName("each entry covers exactly `weight` roll values, with no boundary overlap")
    void weightBoundariesAreExact() {
        // The legacy comparison was `chance <= cumulative`, which gave the first entry one extra slot
        // and shifted every boundary after it.
        PtaDropRecord first = one(Items.DIAMOND);
        PtaDropRecord second = one(Items.EMERALD);

        LinkedHashMap<PtaDropRecord, Integer> entries = new LinkedHashMap<>();
        entries.put(first, 3);
        entries.put(second, 2);
        PtaPool pool = PtaPool.create(entries);
        RandomSource random = RandomSource.create(1L);

        assertEquals(Items.DIAMOND, pool.getItemStackForChance(0, random).getItem());
        assertEquals(Items.DIAMOND, pool.getItemStackForChance(2, random).getItem());
        assertEquals(Items.EMERALD, pool.getItemStackForChance(3, random).getItem());
        assertEquals(Items.EMERALD, pool.getItemStackForChance(4, random).getItem());
    }

    @Test
    @DisplayName("a roll at or past the total weight yields nothing")
    void outOfRangeRoll() {
        PtaPool pool = pool(Map.of(one(Items.DIAMOND), 5));
        RandomSource random = RandomSource.create(1L);

        assertTrue(pool.getItemStackForChance(5, random).isEmpty());
        assertTrue(pool.getItemStackForChance(500, random).isEmpty());
    }

    @Test
    @DisplayName("an empty pool yields nothing")
    void emptyPoolYieldsNothing() {
        assertEquals(ItemStack.EMPTY, PtaPool.create(null).getItemStackForChance(0, RandomSource.create(1L)));
    }

    @Test
    @DisplayName("weights hold up over many draws")
    void distributionFollowsWeights() {
        PtaDropRecord common = one(Items.DIAMOND);
        PtaDropRecord rare = one(Items.EMERALD);

        LinkedHashMap<PtaDropRecord, Integer> entries = new LinkedHashMap<>();
        entries.put(common, 90);
        entries.put(rare, 10);
        PtaPool pool = PtaPool.create(entries);

        RandomSource random = RandomSource.create(20260728L);
        int rareCount = 0;
        int draws = 20000;
        for (int i = 0; i < draws; i++) {
            if (pool.getItemStackForChance(random.nextInt(100), random).getItem() == Items.EMERALD) {
                rareCount++;
            }
        }

        double share = (double) rareCount / draws;
        assertTrue(share > 0.08 && share < 0.12, "expected ~10% rare, got " + share);
    }

    @Test
    @DisplayName("the displayed slot count matches the entries the viewers lay out")
    void poolSizeCountsWhatIsDrawn() {
        // JEI and EMI both lay out `!record.isEmpty()` entries. Counting anything else here sized the
        // category background wrong and pushed the last slots outside it.
        LinkedHashMap<PtaDropRecord, Integer> entries = new LinkedHashMap<>();
        entries.put(one(Items.DIAMOND), 1);
        entries.put(new PtaDropRecord(items(Items.AIR), 1, 1, null), 1);   // filler: empty
        entries.put(new PtaDropRecord(items(Items.EMERALD), 0, 3, null), 1); // [0,3]: drawn
        entries.put(new PtaDropRecord(items(Items.GOLD_INGOT), 0, 0, null), 1); // never drops

        assertEquals(2, PtaPool.create(entries).getTotalPoolSize());
    }

    @Test
    @DisplayName("row count is the drop count in rows of nine")
    void rowCount() {
        assertEquals(0, PtaPool.create(new LinkedHashMap<>()).getJEIRowCount());

        LinkedHashMap<PtaDropRecord, Integer> nine = new LinkedHashMap<>();
        for (int i = 0; i < 9; i++) {
            nine.put(new PtaDropRecord(items(Items.DIAMOND), 1, i + 1, null), 1);
        }
        assertEquals(1, PtaPool.create(nine).getJEIRowCount());

        nine.put(new PtaDropRecord(items(Items.EMERALD), 1, 1, null), 1);
        assertEquals(2, PtaPool.create(nine).getJEIRowCount());
    }
}
