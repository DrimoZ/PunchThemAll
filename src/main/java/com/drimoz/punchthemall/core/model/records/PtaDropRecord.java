package com.drimoz.punchthemall.core.model.records;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashSet;
import java.util.Set;

public record PtaDropRecord(Set<Item> items, int min, int max, CompoundTag nbt) {

    // Calculated Properties

    public boolean isEmpty() {
        return items.isEmpty() || min == 0;
    }

    // Life cycle

    public PtaDropRecord(Set<Item> items, int min, int max, CompoundTag nbt) {
        this.items = items == null ? new HashSet<>() : items;

        this.min = Math.max(0, min);
        this.max = this.min == 0 ? 0 : Math.max(this.min, max);
        this.nbt = this.min == 0 || nbt == null ? new CompoundTag() : nbt;
    }

    // Interface

    public ItemStack getItemStack() {
        return isEmpty() ? ItemStack.EMPTY : new ItemStack(pickRandomItem(), calculateCount(), nbt);
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
