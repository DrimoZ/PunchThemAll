package com.drimoz.punchthemall.core.model.classes;

import com.drimoz.punchthemall.core.model.records.PtaDropRecord;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * The full reward description for an interaction (schema_version 2, §5.5): a weighted
 * {@link PtaPool} plus optional {@code guaranteed} drops, multiple {@code rolls}, and a
 * Fortune/Looting-style bonus.
 *
 * <p>The default {@link #of(PtaPool)} form (1 roll, no guaranteed, no fortune) reproduces the legacy
 * single-pick behaviour exactly, and {@link #getPool()} still exposes the weighted pool so JEI and
 * existing code keep working unchanged.</p>
 */
public class PtaRewards {

    private final PtaPool pool;
    private final List<PtaDropRecord> guaranteed;
    private final int rolls;
    private final Enchantment fortuneEnchant; // null = no fortune bonus
    private final double fortuneFactor;

    private PtaRewards(PtaPool pool, List<PtaDropRecord> guaranteed, int rolls, Enchantment fortuneEnchant, double fortuneFactor) {
        this.pool = pool == null ? PtaPool.create(null) : pool;
        this.guaranteed = guaranteed == null ? List.of() : guaranteed;
        this.rolls = Math.max(0, rolls);
        this.fortuneEnchant = fortuneEnchant;
        this.fortuneFactor = Math.max(0, fortuneFactor);
    }

    public static PtaRewards of(PtaPool pool) {
        return new PtaRewards(pool, List.of(), 1, null, 0);
    }

    public static PtaRewards create(PtaPool pool, List<PtaDropRecord> guaranteed, int rolls, Enchantment fortuneEnchant, double fortuneFactor) {
        return new PtaRewards(pool, guaranteed, rolls, fortuneEnchant, fortuneFactor);
    }

    public PtaPool getPool() {
        return pool;
    }

    public List<PtaDropRecord> getGuaranteed() {
        return guaranteed;
    }

    public int getRolls() {
        return rolls;
    }

    public boolean hasGuaranteed() {
        return guaranteed.stream().anyMatch(record -> !record.isEmpty());
    }

    public boolean hasFortune() {
        return fortuneEnchant != null && fortuneFactor > 0;
    }

    public Enchantment getFortuneEnchant() {
        return fortuneEnchant;
    }

    public double getFortuneFactor() {
        return fortuneFactor;
    }

    // Number of non-empty drops shown as slots in JEI (weighted pool + guaranteed).
    public int getJeiDropCount() {
        int guaranteedCount = (int) guaranteed.stream().filter(record -> !record.isEmpty()).count();
        return pool.getTotalPoolSize() + guaranteedCount;
    }

    public int getJeiRowCount() {
        int count = getJeiDropCount();
        return count % 9 == 0 ? count / 9 : count / 9 + 1;
    }

    /**
     * Produce the drops for one successful interaction: every guaranteed entry, then {@code rolls}
     * weighted picks, with a Fortune-style bonus added to the weighted picks based on the held item.
     */
    public List<ItemStack> roll(RandomSource random, ItemStack handItem) {
        List<ItemStack> results = new ArrayList<>();

        for (PtaDropRecord entry : guaranteed) {
            ItemStack stack = entry.getItemStack();
            if (!stack.isEmpty()) results.add(stack);
        }

        int totalWeight = pool.getTotalPoolWeight();
        int bonus = fortuneBonus(handItem);
        for (int i = 0; i < rolls && totalWeight > 0; i++) {
            ItemStack stack = pool.getItemStackForChance(random.nextInt(totalWeight));
            if (!stack.isEmpty()) {
                if (bonus > 0) stack.grow(bonus);
                results.add(stack);
            }
        }

        return results;
    }

    private int fortuneBonus(ItemStack handItem) {
        if (fortuneEnchant == null || fortuneFactor <= 0 || handItem == null || handItem.isEmpty()) return 0;
        int level = EnchantmentHelper.getItemEnchantmentLevel(fortuneEnchant, handItem);
        return level <= 0 ? 0 : (int) Math.round(level * fortuneFactor);
    }

    @Override
    public String toString() {
        return "PtaRewards{pool=" + pool + ", guaranteed=" + guaranteed + ", rolls=" + rolls
                + ", fortuneEnchant=" + fortuneEnchant + ", fortuneFactor=" + fortuneFactor + '}';
    }
}
