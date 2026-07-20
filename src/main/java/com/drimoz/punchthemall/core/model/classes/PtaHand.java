package com.drimoz.punchthemall.core.model.classes;

import com.drimoz.punchthemall.core.model.enums.PtaHandEnum;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PtaHand {

    private final PtaHandEnum hand;
    private final Set<Item> itemSet;
    private final CompoundTag nbtWhiteList;
    private final CompoundTag nbtBlackList;
    private final List<PtaNbtPredicate> nbtPredicates;
    private final double chance;
    private final boolean damageable;
    private final boolean consumable;

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

    public boolean hasNbtPredicates() {
        return !isEmpty() && !nbtPredicates.isEmpty();
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

    public List<PtaNbtPredicate> getNbtPredicates() {
        return nbtPredicates;
    }

    public double getChance() {
        return isEmpty() ? 0 : chance;
    }

    public boolean isDamageable() {
        return !isEmpty() && chance > 0 && damageable;
    }

    public boolean isConsumable() {
        return !isEmpty() && chance > 0 && consumable;
    }

    // Life Cycle

    public static PtaHand create(PtaHandEnum hand, Set<Item> itemSet, CompoundTag nbtWhiteList, CompoundTag nbtBlackList) {
        return new PtaHand(hand, itemSet, nbtWhiteList, nbtBlackList, List.of(), 0, false, false);
    }

    public static PtaHand create(PtaHandEnum hand, Set<Item> itemSet, CompoundTag nbtWhiteList, CompoundTag nbtBlackList, double chance, boolean damageable, boolean consumed) {
        return new PtaHand(hand, itemSet, nbtWhiteList, nbtBlackList, List.of(), chance, damageable, consumed);
    }

    public static PtaHand create(PtaHandEnum hand, Set<Item> itemSet, CompoundTag nbtWhiteList, CompoundTag nbtBlackList, List<PtaNbtPredicate> nbtPredicates, double chance, boolean damageable, boolean consumed) {
        return new PtaHand(hand, itemSet, nbtWhiteList, nbtBlackList, nbtPredicates, chance, damageable, consumed);
    }

    public static PtaHand createEmpty(PtaHandEnum hand) {
        return new PtaHand(hand, null, null, null, List.of(), 0, false, false);
    }

    protected PtaHand(PtaHandEnum hand, Set<Item> itemSet, CompoundTag nbtWhiteList, CompoundTag nbtBlackList, List<PtaNbtPredicate> nbtPredicates, double chance, boolean damageable, boolean consumable) {
        this.hand = hand;
        this.chance = chance < 0 ? 0 : chance > 1 ? 1 : chance;
        this.damageable = damageable;
        this.consumable = consumable;

        if (itemSet == null || itemSet.isEmpty()) {
            this.itemSet = itemSet != null ? itemSet : new HashSet<>();
            this.nbtWhiteList = new CompoundTag();
            this.nbtBlackList = new CompoundTag();
            this.nbtPredicates = List.of();
        } else {
            this.itemSet = itemSet;
            this.nbtWhiteList = nbtWhiteList == null ? new CompoundTag() : nbtWhiteList;
            this.nbtBlackList = nbtBlackList == null ? new CompoundTag() : nbtBlackList;
            this.nbtPredicates = nbtPredicates == null ? List.of() : nbtPredicates;
        }
    }

    // Interface

    public List<ItemStack> getStacks() {
        return itemSet.stream().map(ItemStack::new).toList();
    }

    public boolean shouldConsume(RandomSource random) {
        return !isEmpty() && chance > 0 && random.nextDouble() <= chance;
    }

    @Override
    public String toString() {
        return "PtaHand{hand=" + hand + ", itemSet=" + itemSet + ", chance=" + chance + ", damageable=" + damageable + ", consumable=" + consumable + '}';
    }
}
