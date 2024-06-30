package com.drimoz.punchthemall.core.model.classes;


import com.drimoz.punchthemall.core.model.enums.PtaHandEnum;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author drimoz
 */

public class PtaHand {

    // Private Properties

    private final PtaHandEnum hand;
    private final Set<Item> itemSet;
    private final CompoundTag nbtWhiteList;
    private final CompoundTag nbtBlackList;
    private final double chance;
    private final boolean damageable;
    private final boolean consumed;

    // Calculated Properties

    public boolean isEmpty() {
        return itemSet.isEmpty();
    }

    public boolean useAnyHand() {
        return !isEmpty() && hand == PtaHandEnum.ANY_HAND;
    }

    public boolean useMainHand() {
        return !isEmpty() && hand == PtaHandEnum.MAIN_HAND;
    }

    public boolean useOffHand() {
        return !isEmpty() && hand == PtaHandEnum.OFF_HAND;
    }

    public boolean hasNbtWhiteList() {
        return !isEmpty() && !nbtWhiteList.isEmpty();
    }

    public boolean hasNbtBlackList() {
        return !isEmpty() && !nbtBlackList.isEmpty();
    }

    // Getters

    public PtaHandEnum getHand() {
        return isEmpty() ? null : hand;
    }

    public Set<Item> getItemSet() {
        return itemSet;
    }

    public CompoundTag getNbtWhiteList() {
        return nbtWhiteList;
    }

    public CompoundTag getNbtBlackList() {
        return nbtBlackList;
    }

    public double getChance() {
        return isEmpty() ? 0 : chance;
    }

    public boolean isDamageable() {
        return !isEmpty() && chance > 0 && damageable;
    }

    public boolean isConsumed() {
        return !isEmpty() && chance > 0 && consumed;
    }

    // Life Cycle

    public static PtaHand create(PtaHandEnum hand, Set<Item> itemSet, CompoundTag nbtWhiteList, CompoundTag nbtBlackList) {
        return new PtaHand(hand, itemSet, nbtWhiteList, nbtBlackList, 0, false, false);
    }

    public static PtaHand create(PtaHandEnum hand, Set<Item> itemSet, CompoundTag nbtWhiteList, CompoundTag nbtBlackList, double chance, boolean damageable, boolean consumed) {
        return new PtaHand(hand, itemSet, nbtWhiteList, nbtBlackList, chance, damageable, consumed);
    }

    public static PtaHand createEmpty() {
        return new PtaHand(null, null, null, null, 0, false, false);
    }

    protected PtaHand(PtaHandEnum hand, Set<Item> itemSet,  CompoundTag nbtWhiteList, CompoundTag nbtBlackList, double chance, boolean damageable, boolean consumed) {
        this.hand = hand;
        this.chance = chance < 0 ? 0 : chance > 1 ? 1 : chance;
        this.damageable = damageable;
        this.consumed = consumed;

        if (itemSet == null || itemSet.isEmpty()) {
            this.itemSet = itemSet != null ? itemSet : new HashSet<>();
            this.nbtWhiteList = new CompoundTag();
            this.nbtBlackList = new CompoundTag();
        }
        else {
            this.itemSet = itemSet;
            this.nbtWhiteList = nbtWhiteList == null ? new CompoundTag() : nbtWhiteList;
            this.nbtBlackList = nbtBlackList == null ? new CompoundTag() : nbtBlackList;
        }
    }

    // Interface

    public List<ItemStack> getStacks() {
        return itemSet.stream().map(ItemStack::new).toList();
    }

    public boolean shouldConsume() {
        return !isEmpty() && chance > 0 && Math.random() < chance;
    }

    // Interface ( Util )

    @Override
    public String toString() {
        return "PtaHand{" +
                "hand=" + hand +
                ", itemSet=" + itemSet +
                ", nbtWhiteList=" + nbtWhiteList +
                ", nbtBlackList=" + nbtBlackList +
                ", chance=" + chance +
                ", damageable=" + damageable +
                ", consumed=" + consumed +
                '}';
    }


}
