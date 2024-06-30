package com.drimoz.punchthemall.core.model.classes;

import com.drimoz.punchthemall.core.model.records.PtaStateRecord;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import java.util.HashSet;
import java.util.Set;

/**
 * @author drimoz
 */

public class PtaTransformation {

    // Private Properties

    private final double chance;
    private final Block block;
    private final Fluid fluid;
    private final Set<PtaStateRecord<?>> stateList;
    private final CompoundTag nbtList;

    // Calculated Properties

    public boolean hasTransformation() {
        return chance > 0;
    }

    public boolean isAir() {
        return block == null && fluid == null;
    }

    public boolean isBlock() {
        return block != null && fluid == null;
    }

    public boolean isFluid() {
        return block == null && fluid != null;
    }

    public boolean hasStateList() {
        return !stateList.isEmpty();
    }

    public boolean hasNbtList() {
        return !nbtList.isEmpty();
    }

    // Getters


    public double getChance() {
        return chance;
    }

    public Block getBlock() {
        return block;
    }

    public Fluid getFluid() {
        return fluid;
    }

    public Set<PtaStateRecord<?>> getStateList() {
        return stateList;
    }

    public CompoundTag getNbtList() {
        return nbtList;
    }

    // Life cycle

    public static PtaTransformation createBlock (double chance, Block block, Set<PtaStateRecord<?>> stateList, CompoundTag nbtList) {
        return new PtaTransformation(chance, block, null, stateList, nbtList);
    }

    public static PtaTransformation createFluid (double chance, Fluid fluid, Set<PtaStateRecord<?>> stateList, CompoundTag nbtList) {
        return new PtaTransformation(chance, null, fluid, stateList, nbtList);
    }

    public static PtaTransformation createAir (double chance) {
        return new PtaTransformation(chance, null,null,null,null);
    }

    protected PtaTransformation (
            double chance,
            Block block, Fluid fluid,
            Set<PtaStateRecord<?>> stateList, CompoundTag nbtList
    ) {
        if (block != null && fluid != null)
            throw new IllegalArgumentException("Transformation must be either a Fluid or a Block.");

        this.chance = chance < 0 ? 0 : chance > 1 ? 1 : chance;

        if (block == null && fluid == null) {
            this.block = null;
            this.fluid = null;
            this.stateList = new HashSet<>();
            this.nbtList = new CompoundTag();
        }
        else {
            this.block = null;
            this.fluid = null;
            this.stateList = stateList == null ? new HashSet<>() : stateList;
            this.nbtList = nbtList == null ? new CompoundTag() : nbtList;
        }
    }

    // Interface



    // Interface ( Util )

    @Override
    public String toString() {
        return "PtaTransformation{" +
                "chance=" + chance +
                ", block=" + block +
                ", fluid=" + fluid +
                ", stateList=" + stateList +
                ", nbtList=" + nbtList +
                '}';
    }
}
