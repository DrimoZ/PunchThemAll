package com.drimoz.punchthemall.core.model;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class InteractionHand {

    // Private properties

    private final EInteractionHand handType;
    private final ItemStack item;
    private final CompoundTag nbt;
    private final boolean damageable;
    private final boolean consumed;

    // Life cycle
    public InteractionHand(EInteractionHand handType, ItemStack item, CompoundTag nbt, boolean damageable, boolean consumed) {
        if (!handType.equals(EInteractionHand.ANY_HAND)) {
            if (item == null) throw new IllegalArgumentException("Missing Item for given Interaction Hand");
        }

        this.handType = handType;
        this.item = item;
        this.nbt = nbt == null ? new CompoundTag() : nbt;
        this.damageable = damageable;
        this.consumed = consumed;
    }

    // Interface ( Getters )

    public EInteractionHand getHandType() {
        return handType;
    }

    public ItemStack getItemStack() {
        return item;
    }

    public CompoundTag getNbt() {
        return nbt;
    }

    public boolean isDamageable() {
        return damageable;
    }

    public boolean isConsumed() {
        return consumed;
    }

    // Interface ( Others )

    @Override
    public String toString() {
        return "InteractionHand{" +
                "\n\thand_type=" + handType +
                ", \n\titem=" + item +
                ", \n\tdamageable=" + damageable +
                '}';
    }
}
