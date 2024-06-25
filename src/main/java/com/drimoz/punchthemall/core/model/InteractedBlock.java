package com.drimoz.punchthemall.core.model;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

public class InteractedBlock {
    private final EInteractionBlock blockType;
    private final Object state; // Can be BlockState or FluidState
    private final Double transformationChance;
    private final EInteractionBlock transformedType;
    private final Object transformedState; // Can be BlockState or FluidState

    public InteractedBlock(EInteractionBlock blockType) {
        this(blockType, null, 0., null, null);
    }

    public InteractedBlock(EInteractionBlock blockType, Object state) {
        this(blockType, state, 0., null, null);
    }

    // Constructor
    public InteractedBlock(
            EInteractionBlock blockType, Object state,
            Double transformationChance, EInteractionBlock transformedType, Object transformedState
    ) {
        if (state != null && !(state instanceof BlockState) && !(state instanceof FluidState)) {
            throw new IllegalArgumentException("block must be either a Block or Fluid.");
        }
        if (transformedState != null && !(transformedState instanceof BlockState) && !(transformedState instanceof FluidState)) {
            throw new IllegalArgumentException("transformedBlock must be either a Block or Fluid.");
        }
        this.blockType = blockType;
        this.state = state;

        if (transformationChance == null || transformationChance < 0) transformationChance = 0.;
        if (transformationChance > 1) transformationChance = 1.;

        this.transformationChance = transformationChance;
        this.transformedType = transformedType;
        this.transformedState = transformedState;
    }

    // Getters
    public EInteractionBlock getBlockType() {
        return blockType;
    }

    public Object getState() {
        return state;
    }

    public Double getTransformationChance() {
        return transformationChance;
    }

    public EInteractionBlock getTransformedType() {
        return transformedType;
    }

    public Object getTransformedState() {
        return transformedState;
    }

    // Utility Methods to get Block or Fluid
    public Block getBlockAsBlock() {
        return state instanceof BlockState ? ((BlockState)state).getBlock() : null;
    }

    public BlockState getBlockAsBlockState() {
        return state instanceof BlockState ? ((BlockState)state) : null;
    }

    public Fluid getBlockAsFluid() {
        return state instanceof FluidState ? ((FluidState)state).getType() : null;
    }

    public FluidState getBlockAsFluidState() {
        return state instanceof FluidState ? ((FluidState)state) : null;
    }

    public Block getTransformedBlockAsBlock() {
        return transformedState instanceof BlockState ? ((BlockState)transformedState).getBlock() : null;
    }

    public BlockState getTransformedBlockAsBlockState() {
        return transformedState instanceof BlockState ? ((BlockState)transformedState) : null;
    }

    public Fluid getTransformedBlockAsFluid() {
        return transformedState instanceof FluidState ? ((FluidState)transformedState).getType() : null;
    }

    public FluidState getTransformedBlockAsFluidState() {
        return transformedState instanceof FluidState ? ((FluidState)transformedState) : null;
    }

    @Override
    public String toString() {
        return "InteractedBlock{" +
                "blockType=" + blockType +
                ", state=" + state +
                ", transformationChance=" + transformationChance +
                ", transformationType=" + transformedType +
                ", transformedState=" + transformedState +
                '}';
    }
}

