package com.drimoz.punchthemall.core.model.classes;


import com.drimoz.punchthemall.core.model.records.PtaStateRecord;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author drimoz
 */

public class PtaBlock {

    // Private Properties

    private final Set<Block> blockSet;
    private final Set<Fluid> fluidSet;
    private final Set<PtaStateRecord<?>> stateWhiteList;
    private final Set<PtaStateRecord<?>> stateBlackList;
    private final CompoundTag nbtWhiteList;
    private final CompoundTag nbtBlackList;

    // Calculated Properties

    public boolean isAir() {
        return blockSet.isEmpty() && fluidSet.isEmpty();
    }

    public boolean isBlock() {
        return !blockSet.isEmpty() && fluidSet.isEmpty();
    }

    public boolean isFluid() {
        return blockSet.isEmpty() && !fluidSet.isEmpty();
    }

    public boolean hasStateWhiteList() {
        return !stateWhiteList.isEmpty();
    }

    public boolean hasStateBlackList() {
        return !stateBlackList.isEmpty();
    }

    public boolean hasNbtWhiteList() {
        return !nbtWhiteList.isEmpty();
    }

    public boolean hasNbtBlackList() {
        return !nbtBlackList.isEmpty();
    }

    // Getters

    public Set<Block> getBlockSet() {
        return blockSet;
    }

    public Set<Fluid> getFluidSet() {
        return fluidSet;
    }

    public Set<PtaStateRecord<?>> getStateWhiteList() {
        return stateWhiteList;
    }

    public Set<PtaStateRecord<?>> getStateBlackList() {
        return stateBlackList;
    }

    public CompoundTag getNbtWhiteList() {
        return nbtWhiteList;
    }

    public CompoundTag getNbtBlackList() {
        return nbtBlackList;
    }

    // Life cycle

    public static PtaBlock createBlock (Set<Block> blockSet, Set<PtaStateRecord<?>> stateWhiteList, Set<PtaStateRecord<?>> stateBlackList, CompoundTag nbtWhiteList, CompoundTag nbtBlackList) {
        return new PtaBlock(blockSet, null, stateWhiteList, stateBlackList, nbtWhiteList, nbtBlackList);
    }

    public static PtaBlock createFluid (Set<Fluid> fluidSet, Set<PtaStateRecord<?>> stateWhiteList, Set<PtaStateRecord<?>> stateBlackList, CompoundTag nbtWhiteList, CompoundTag nbtBlackList) {
        return new PtaBlock(null, fluidSet, stateWhiteList, stateBlackList, nbtWhiteList, nbtBlackList);
    }

    public static PtaBlock createAir () {
        return new PtaBlock(null, null,null,null,null,null);
    }

    protected PtaBlock(
            Set<Block> blockSet, Set<Fluid> fluidSet,
            Set<PtaStateRecord<?>> stateWhiteList, Set<PtaStateRecord<?>> stateBlackList,
            CompoundTag nbtWhiteList, CompoundTag nbtBlackList
    ) {
        if (blockSet != null && !blockSet.isEmpty() && fluidSet != null && !fluidSet.isEmpty())
            throw new IllegalArgumentException("Block must be either a Fluid or a Block.");

        // Air
        if (blockSet == null && fluidSet == null) {
            this.blockSet = new HashSet<>();
            this.fluidSet = new HashSet<>();
            this.stateWhiteList = new HashSet<>();
            this.stateBlackList = new HashSet<>();
            this.nbtWhiteList = new CompoundTag();
            this.nbtBlackList = new CompoundTag();
        }

        // Fluid or Block
        else {
            this.blockSet = blockSet == null ? new HashSet<>() : blockSet;
            this.fluidSet = fluidSet == null ? new HashSet<>() : fluidSet;
            this.stateWhiteList = stateWhiteList == null ? new HashSet<>() : stateWhiteList;
            this.stateBlackList = stateBlackList == null ? new HashSet<>() : stateBlackList;
            this.nbtWhiteList = nbtWhiteList == null ? new CompoundTag() : nbtWhiteList;
            this.nbtBlackList = nbtBlackList == null ? new CompoundTag() : nbtBlackList;
        }
    }

    // Interface

    public List<ItemStack> getBlockStacks() {
        if (isAir() || isFluid()) return new ArrayList<>();
        else return blockSet.stream().map(ItemStack::new).toList();
    }

    public List<ItemStack> getFluidStacks() {
        if (isAir() || isFluid()) return new ArrayList<>();
        else return fluidSet.stream().map(fluid -> new ItemStack(fluid.getBucket())).toList();
    }

    public Fluid getFluid() {
        return fluidSet.stream().findFirst().orElse(null);
    }

    public boolean isBlockFromSet(Block block) {
        return isBlock() && blockSet.contains(block);
    }

    public boolean isFluidFromSet(Fluid fluid) {
        return isFluid() && fluidSet.contains(fluid);
    }

    // Interface ( Util )

    @Override
    public String toString() {
        return "PtaBlock{" +
                "blockSet=" + blockSet +
                ", fluidSet=" + fluidSet +
                ", stateWhiteList=" + stateWhiteList +
                ", stateBlackList=" + stateBlackList +
                ", nbtWhiteList=" + nbtWhiteList +
                ", nbtBlackList=" + nbtBlackList +
                '}';
    }
}
