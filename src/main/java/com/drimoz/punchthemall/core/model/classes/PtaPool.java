package com.drimoz.punchthemall.core.model.classes;

import com.drimoz.punchthemall.core.model.records.PtaDropRecord;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

public class PtaPool {

    // Private Properties

    private final Map<PtaDropRecord, Integer> dropPool;

    // Calculated Properties

    public int getTotalPoolWeight() {
        return this.dropPool.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getTotalPoolSize() {
        return (int) this.dropPool.keySet().stream().filter(a -> !a.isEmpty() && a.items().stream().allMatch(item -> item.equals(Items.AIR))).count();
    }

    public boolean isEmpty() {
        return dropPool.isEmpty() || getTotalPoolWeight() == 0;
    }

    // Getters

    public Map<PtaDropRecord, Integer> getDropPool() {
        return dropPool;
    }

    // Life cycle

    public static PtaPool create(Map<PtaDropRecord, Integer> dropPool) {
        return new PtaPool(dropPool);
    }

    protected PtaPool(Map<PtaDropRecord, Integer> dropPool) {
        this.dropPool = dropPool == null ? new HashMap<>() : dropPool;
    }

    // Interface

    public ItemStack getRandomItemStack() {
        return getItemStackForChance((int) (Math.random() * getTotalPoolWeight()));
    }

    public ItemStack getItemStackForChance(int chance) {
        if (isEmpty()) return ItemStack.EMPTY;

        int cumulativeChance = 0;

        for (Map.Entry<PtaDropRecord, Integer> drop : shuffledPool().entrySet()) {
            cumulativeChance += drop.getValue();

            if (chance <= cumulativeChance) {
                return drop.getKey().getItemStack();
            }
        }

        return ItemStack.EMPTY;
    }

    public int getJEIRowCount() {
        return getTotalPoolSize() % 9 == 0 ? getTotalPoolSize() / 9 : getTotalPoolSize() / 9 + 1;
    }

    // Interface ( Util )

    @Override
    public String toString() {
        return "PtaPool{" +
                "dropPool=" + dropPool +
                '}';
    }

    // Inner Work

    protected Map<PtaDropRecord, Integer> shuffledPool() {
        List<Map.Entry<PtaDropRecord, Integer>> entries = new ArrayList<>(this.dropPool.entrySet());
        Collections.shuffle(entries);

        Map<PtaDropRecord, Integer> shuffledMap = new HashMap<>();
        for (Map.Entry<PtaDropRecord, Integer> entry : entries) {
            shuffledMap.put(entry.getKey(), entry.getValue());
        }

        return shuffledMap;
    }
}
