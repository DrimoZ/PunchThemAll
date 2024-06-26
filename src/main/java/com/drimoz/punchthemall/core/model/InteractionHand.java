package com.drimoz.punchthemall.core.model;

import net.minecraft.world.item.ItemStack;

public class InteractionHand {

    // Private properties

    private final EInteractionHand hand_type;
    private final ItemStack item;
    private final boolean damageable;

    // Life cycle

    public InteractionHand(EInteractionHand handType) {
        this(handType, ItemStack.EMPTY, false);
    }

    public InteractionHand(EInteractionHand handType, ItemStack item) {
        this(handType, item, false);
    }

    public InteractionHand(EInteractionHand hand_type, ItemStack item, boolean damageable) {

        if (!hand_type.equals(EInteractionHand.ANY_HAND)) {
            if (item == null) throw new IllegalArgumentException("Missing Item for given Interaction Hand");
        }

        this.hand_type = hand_type;
        this.item = item;
        this.damageable = damageable;
    }

    // Interface ( Getters )

    public EInteractionHand getInteractionHandType() {
        return hand_type;
    }

    public ItemStack getItemStack() {
        return item;
    }

    public boolean isDamageable() {
        return damageable;
    }

    // Interface ( Others )

    @Override
    public String toString() {
        return "InteractionHand{" +
                "\n\thand_type=" + hand_type +
                ", \n\titem=" + item +
                ", \n\tdamageable=" + damageable +
                '}';
    }
}
