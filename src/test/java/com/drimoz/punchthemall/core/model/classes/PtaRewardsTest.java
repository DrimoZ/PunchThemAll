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
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The full reward description: weighted pool, guaranteed drops, rolls and the Fortune bonus. */
class PtaRewardsTest {

    @BeforeAll
    static void bootstrap() {
        McBootstrap.ensure();
    }

    private static Set<Item> items(Item... items) {
        return new HashSet<>(Set.of(items));
    }

    private static PtaDropRecord exactly(Item item, int count) {
        return new PtaDropRecord(items(item), count, count, null);
    }

    private static PtaPool singleEntryPool(Item item, int count) {
        LinkedHashMap<PtaDropRecord, Integer> entries = new LinkedHashMap<>();
        entries.put(exactly(item, count), 1);
        return PtaPool.create(entries);
    }

    private static RandomSource seeded() {
        return RandomSource.create(4242L);
    }

    @Test
    @DisplayName("the legacy shape is one roll, no guarantees, no fortune")
    void legacyDefaults() {
        PtaRewards rewards = PtaRewards.of(singleEntryPool(Items.DIAMOND, 1));

        assertEquals(1, rewards.getRolls());
        assertFalse(rewards.hasGuaranteed());
        assertFalse(rewards.hasFortune());
        assertEquals(1, rewards.roll(seeded(), ItemStack.EMPTY).size());
    }

    @Test
    @DisplayName("rolls and fortune factor are clamped to sane values")
    void constructorClamps() {
        PtaRewards rewards = PtaRewards.create(singleEntryPool(Items.DIAMOND, 1), List.of(), -5, null, -2);

        assertEquals(0, rewards.getRolls());
        assertEquals(0, rewards.getFortuneFactor());
        assertFalse(rewards.hasFortune());
    }

    @Test
    @DisplayName("zero rolls draw nothing from the pool")
    void zeroRolls() {
        PtaRewards rewards = PtaRewards.create(singleEntryPool(Items.DIAMOND, 1), List.of(), 0, null, 0);
        assertTrue(rewards.roll(seeded(), ItemStack.EMPTY).isEmpty());
    }

    @Test
    @DisplayName("each roll draws once from the pool")
    void multipleRolls() {
        PtaRewards rewards = PtaRewards.create(singleEntryPool(Items.DIAMOND, 1), List.of(), 3, null, 0);
        assertEquals(3, rewards.roll(seeded(), ItemStack.EMPTY).size());
    }

    @Test
    @DisplayName("guaranteed drops come out on every success, alongside the rolls")
    void guaranteedAlwaysDrop() {
        PtaRewards rewards = PtaRewards.create(
                singleEntryPool(Items.DIAMOND, 1),
                List.of(exactly(Items.EMERALD, 2)),
                1, null, 0);

        List<ItemStack> drops = rewards.roll(seeded(), ItemStack.EMPTY);

        assertEquals(2, drops.size());
        assertTrue(drops.stream().anyMatch(stack -> stack.getItem() == Items.EMERALD && stack.getCount() == 2));
        assertTrue(drops.stream().anyMatch(stack -> stack.getItem() == Items.DIAMOND));
        assertTrue(rewards.hasGuaranteed());
    }

    @Test
    @DisplayName("guaranteed drops work with an empty pool")
    void guaranteedWithoutPool() {
        PtaRewards rewards = PtaRewards.create(
                PtaPool.create(null), List.of(exactly(Items.EMERALD, 1)), 1, null, 0);

        assertEquals(1, rewards.roll(seeded(), ItemStack.EMPTY).size());
    }

    @Test
    @DisplayName("an empty guaranteed entry is not reported as a guarantee")
    void emptyGuaranteedIgnored() {
        PtaRewards rewards = PtaRewards.create(
                PtaPool.create(null), List.of(new PtaDropRecord(items(Items.AIR), 1, 1, null)), 1, null, 0);

        assertFalse(rewards.hasGuaranteed());
        assertTrue(rewards.roll(seeded(), ItemStack.EMPTY).isEmpty());
    }

    @Test
    @DisplayName("no fortune enchantment means no bonus, whatever the held item")
    void noFortuneNoBonus() {
        PtaRewards rewards = PtaRewards.create(singleEntryPool(Items.DIAMOND, 1), List.of(), 1, null, 5);

        List<ItemStack> drops = rewards.roll(seeded(), new ItemStack(Items.DIAMOND_PICKAXE));

        assertEquals(1, drops.size());
        assertEquals(1, drops.get(0).getCount());
    }

    @Test
    @DisplayName("a drop never exceeds the item's maximum stack size")
    void neverExceedsMaxStackSize() {
        // Fortune used to grow() unconditionally. An over-sized stack survives in an ItemEntity but
        // is clamped the moment it enters an inventory, so the surplus vanished silently.
        PtaRewards rewards = PtaRewards.create(singleEntryPool(Items.DIAMOND, 64), List.of(), 1, null, 0);

        for (ItemStack stack : rewards.roll(seeded(), ItemStack.EMPTY)) {
            assertTrue(stack.getCount() <= stack.getMaxStackSize());
        }
    }

    @Test
    @DisplayName("the viewers count weighted and guaranteed drops together")
    void jeiCounts() {
        LinkedHashMap<PtaDropRecord, Integer> entries = new LinkedHashMap<>();
        entries.put(exactly(Items.DIAMOND, 1), 1);
        entries.put(exactly(Items.EMERALD, 1), 1);

        PtaRewards rewards = PtaRewards.create(
                PtaPool.create(entries), List.of(exactly(Items.GOLD_INGOT, 1)), 1, null, 0);

        assertEquals(3, rewards.getJeiDropCount());
        assertEquals(1, rewards.getJeiRowCount());
    }

    @Test
    @DisplayName("ten drops need two rows")
    void jeiRowsWrapAtNine() {
        LinkedHashMap<PtaDropRecord, Integer> entries = new LinkedHashMap<>();
        for (int i = 0; i < 10; i++) {
            entries.put(new PtaDropRecord(items(Items.DIAMOND), 1, i + 1, null), 1);
        }

        assertEquals(2, PtaRewards.of(PtaPool.create(entries)).getJeiRowCount());
    }
}
