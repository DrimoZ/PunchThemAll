package com.drimoz.punchthemall.core.model;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class DropEntry {

    // Private properties

    private final ItemStack itemStack;
    private final int chance;

    // Life cycle

    public DropEntry(ItemStack itemStack, int chance) {
        this.itemStack = itemStack;

        if (chance < 1) chance = 1;
        this.chance = chance;
    }

    // Interface ( Getters )

    public ItemStack getItemStack() {
        return itemStack;
    }

    public int getChance() {
        return chance;
    }

    // Interface ( Others )

    public boolean isItemEmpty() {
        return this.itemStack.isEmpty() || this.itemStack.is(Items.AIR) || this.itemStack.getItem().equals(Items.AIR);
    }

    @Override
    public String toString() {
        return "DropEntry{" +
                "\n\titemStack=" + itemStack +
                ", \n\tchance=" + chance +
                '}';
    }
}
