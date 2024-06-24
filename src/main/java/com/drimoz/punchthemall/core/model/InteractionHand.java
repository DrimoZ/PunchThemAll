package com.drimoz.punchthemall.core.model;

import net.minecraft.world.item.ItemStack;

public class InteractionHand {
    private final EInteractionHand hand_type;
    private final ItemStack item;
    private final boolean damageable;


    public InteractionHand(EInteractionHand hand_type) {
        this(hand_type, null, false);
    }

    public InteractionHand(EInteractionHand hand_type, ItemStack item, boolean damageable) {
        this.hand_type = hand_type;
        this.item = item;
        this.damageable = damageable;
    }

    public EInteractionHand getHand_type() {
        return hand_type;
    }

    public ItemStack getItem() {
        return item;
    }

    public boolean isDamageable() {
        return damageable;
    }

    @Override
    public String toString() {
        return "InteractionHand{" +
                "hand_type=" + hand_type +
                ", item=" + item +
                ", damageable=" + damageable +
                '}';
    }
}
