package com.drimoz.punchthemall.core.model.records;

import com.drimoz.punchthemall.core.util.ItemView;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
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
     * silently deleted the entry from both the drop roll and the viewers.
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

    public ItemStack getItemStack(RandomSource random) {
        if (isEmpty()) return ItemStack.EMPTY;
        int count = calculateCount(random);
        // A [0, n] range legitimately rolls a zero; ItemStack would otherwise be constructed empty
        // but non-EMPTY, which callers do not expect.
        if (count <= 0) return ItemStack.EMPTY;

        ItemStack stack = new ItemStack(pickRandomItem(random), count);
        // Apply the authored (stable-view) NBT as 1.21 data components.
        ItemView.applyTo(stack, nbt);
        return stack;
    }

    public int calculateCount(RandomSource random) {
        return min >= max ? min : min + random.nextInt(max - min + 1);
    }

    public Item pickRandomItem(RandomSource random) {
        if (items.isEmpty()) return Items.AIR;
        return items.stream().skip(random.nextInt(items.size())).findFirst().orElse(Items.AIR);
    }

    @Override
    public String toString() {
        return "PtaDropRecord{items=" + items + ", min=" + min + ", max=" + max + ", nbt=" + nbt + '}';
    }
}
