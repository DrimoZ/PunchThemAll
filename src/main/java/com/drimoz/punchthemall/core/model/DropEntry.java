package com.drimoz.punchthemall.core.model;

import net.minecraft.world.item.ItemStack;

public class DropEntry {
    private final ItemStack itemStack;
    private final int chance;

    public DropEntry(ItemStack itemStack, int chance) {
        this.itemStack = itemStack;

        if (chance < 1) chance = 1;
        this.chance = chance;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public int getChance() {
        return chance;
    }
}
