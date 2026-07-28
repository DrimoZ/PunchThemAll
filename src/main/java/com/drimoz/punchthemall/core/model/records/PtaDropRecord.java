package com.drimoz.punchthemall.core.model.records;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashSet;
import java.util.Set;

public record PtaDropRecord(Set<Item> items, int min, int max, CompoundTag nbt) {

    // Calculated Properties

    /**
     * Whether this entry can never yield anything. That is decided by {@code max}, not {@code min}:
     * {@code {"min": 0, "max": 3}} is a legitimate "nothing to three" roll, and treating it as empty
     * silently deleted the entry from both the drop roll and the JEI display, while it still took up
     * its weight in the pool.
     */
    public boolean isEmpty() {
        return items.isEmpty() || max == 0 || items.stream().allMatch(item -> item.equals(Items.AIR));
    }

    // Life cycle

    public PtaDropRecord(Set<Item> items, int min, int max, CompoundTag nbt) {
        this.items = items == null ? new HashSet<>() : items;

        this.min = Math.max(0, min);
        this.max = Math.max(this.min, max);
        this.nbt = nbt == null ? new CompoundTag() : nbt;
    }

    // Interface

    public ItemStack getItemStack() {
        if (isEmpty()) return ItemStack.EMPTY;

        // A [0, n] range legitimately rolls a zero; ItemStack would otherwise be constructed with a
        // count of 0, which is empty-but-not-EMPTY and not what callers expect.
        int count = calculateCount();
        if (count <= 0) return ItemStack.EMPTY;

        return new ItemStack(pickRandomItem(), count, nbt);
    }

    public int calculateCount() {
        if (min == max) {
            return min;
        } else {
            return min + (int) (Math.random() * (max - min + 1));
        }
    }

    public Item pickRandomItem() {
        if (items.isEmpty()) return Items.AIR;
        return items.stream().skip((int) (Math.random() * items.size())).findFirst().orElse(Items.AIR);
    }

    // Interface ( Util )

    @Override
    public String toString() {
        return "PtaDropRecord{" +
                "items=" + items +
                ", min=" + min +
                ", max=" + max +
                ", nbt=" + nbt +
                '}';
    }
}
