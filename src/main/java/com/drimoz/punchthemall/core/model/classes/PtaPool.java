package com.drimoz.punchthemall.core.model.classes;

import com.drimoz.punchthemall.core.model.records.PtaDropRecord;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

public class PtaPool {

    private final Map<PtaDropRecord, Integer> dropPool;

    public int getTotalPoolWeight() {
        return this.dropPool.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getTotalPoolSize() {
        return (int) this.dropPool.keySet().stream().filter(a -> !a.isEmpty() && a.items().stream().noneMatch(item -> item.equals(Items.AIR))).count();
    }

    public boolean isEmpty() {
        return dropPool.isEmpty() || getTotalPoolWeight() == 0;
    }

    public Map<PtaDropRecord, Integer> getDropPool() {
        return dropPool;
    }

    public static PtaPool create(Map<PtaDropRecord, Integer> dropPool) {
        return new PtaPool(dropPool);
    }

    protected PtaPool(Map<PtaDropRecord, Integer> dropPool) {
        this.dropPool = dropPool == null ? new HashMap<>() : dropPool;
    }

    /**
     * Weighted pick for a roll value in {@code [0, totalWeight)}. Uses a strict {@code <} comparison
     * so each entry covers exactly {@code weight} slots (fixes the legacy off-by-one boundary bias).
     */
    public ItemStack getItemStackForChance(int chance, RandomSource random) {
        if (isEmpty()) return ItemStack.EMPTY;

        int cumulativeChance = 0;
        for (Map.Entry<PtaDropRecord, Integer> drop : dropPool.entrySet()) {
            cumulativeChance += drop.getValue();
            if (chance < cumulativeChance) {
                return drop.getKey().getItemStack(random);
            }
        }

        return ItemStack.EMPTY;
    }

    public int getJEIRowCount() {
        return getTotalPoolSize() % 9 == 0 ? getTotalPoolSize() / 9 : getTotalPoolSize() / 9 + 1;
    }

    @Override
    public String toString() {
        return "PtaPool{dropPool=" + dropPool + '}';
    }
}
