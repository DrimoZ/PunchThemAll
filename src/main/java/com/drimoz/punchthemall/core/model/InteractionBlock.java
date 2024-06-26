package com.drimoz.punchthemall.core.model;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

import java.util.ArrayList;
import java.util.List;

public class InteractionBlock {

    // Private Properties

    private final Block block;
    private final Fluid fluid;

    private final List<StateEntry<?>> stateEntries;

    private final CompoundTag nbt;

    // Life cycle

    public InteractionBlock(Block block, List<StateEntry<?>> stateEntries, CompoundTag nbt) {
        this(block, null, stateEntries, nbt);
    }

    public InteractionBlock(Fluid fluid, List<StateEntry<?>> stateEntries, CompoundTag nbt) {
        this(null, fluid, stateEntries, nbt);
    }

    private InteractionBlock(
            Block block, Fluid fluid,
            List<StateEntry<?>> stateEntries,
            CompoundTag nbt
    ) {
        if (block != null && fluid != null) throw new IllegalArgumentException("InteractionBlock must be either a Fluid or a Block not both.");

        this.block = block;
        this.fluid = fluid;


        if (stateEntries == null) this.stateEntries = new ArrayList<>();
        else this.stateEntries = stateEntries;

        this.nbt = nbt.copy();
    }

    // Interface ( Getters )

    public Block getBlock() {
        return block;
    }

    public Fluid getFluid() {
        return fluid;
    }

    public List<StateEntry<?>> getStateEntries() {
        return stateEntries;
    }

    public CompoundTag getNbt() {
        return nbt;
    }

    // Interface (  )

    public boolean isBlock() {
        return block != null && fluid == null;
    }

    @Override
    public String toString() {
        return "InteractionBlock{" +
                "\n\tblock=" + block +
                ", \n\tfluid=" + fluid +
                ", \n\tstateEntries=" + stateEntries +
                ", \n\tnbt=" + nbt +
                '}';
    }
}
