package com.drimoz.punchthemall.core.model;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

public class InteractedBlock {
    private final EInteractionBlock blockType;
    private final Object block; // Can be Block or Fluid
    private final Double decayChance;
    private final EInteractionBlock decayType;
    private final Object decayBlock; // Can be Block or Fluid

    public InteractedBlock(EInteractionBlock blockType) {
        this(blockType, null, null, null, null);
    }

    public InteractedBlock(EInteractionBlock blockType, Object block) {
        this(blockType, block, null, null, null);
    }

    // Constructor
    public InteractedBlock(EInteractionBlock blockType, Object block, Double decayChance, EInteractionBlock decayType, Object decayBlock) {
        if (block != null && !(block instanceof Block) && !(block instanceof Fluid)) {
            throw new IllegalArgumentException("block must be either a Block or Fluid.");
        }
        if (decayBlock != null && !(decayBlock instanceof Block) && !(decayBlock instanceof Fluid)) {
            throw new IllegalArgumentException("decayBlock must be either a Block or Fluid.");
        }
        this.blockType = blockType;
        this.block = block;
        this.decayChance = decayChance;
        this.decayType = decayType;
        this.decayBlock = decayBlock;
    }

    // Getters
    public EInteractionBlock getBlockType() {
        return blockType;
    }

    public Object getBlock() {
        return block;
    }

    public Double getDecayChance() {
        return decayChance;
    }

    public EInteractionBlock getDecayType() {
        return decayType;
    }

    public Object getDecayBlock() {
        return decayBlock;
    }

    // Utility Methods to get Block or Fluid
    public Block getBlockAsBlock() {
        return block instanceof Block ? (Block) block : null;
    }

    public Fluid getBlockAsFluid() {
        return block instanceof Fluid ? (Fluid) block : null;
    }

    public Block getDecayBlockAsBlock() {
        return decayBlock instanceof Block ? (Block) decayBlock : null;
    }

    public Fluid getDecayBlockAsFluid() {
        return decayBlock instanceof Fluid ? (Fluid) decayBlock : null;
    }

    @Override
    public String toString() {
        return "InteractedBlock{" +
                "blockType=" + blockType +
                ", block=" + block +
                ", decayChance=" + decayChance +
                ", decayType=" + decayType +
                ", decayBlock=" + decayBlock +
                '}';
    }
}

