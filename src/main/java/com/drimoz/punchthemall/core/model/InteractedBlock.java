package com.drimoz.punchthemall.core.model;

public class InteractedBlock {

    // Private properties

    private final EInteractionBlock blockType;
    private final InteractionBlock blockBase;

    private final double transformationChance;

    private final EInteractionBlock transformedType;
    private final InteractionBlock transformedBase;

    // Life cycle

    public InteractedBlock(EInteractionBlock blockType) {
        this(blockType, null, 0, null, null);
    }

    public InteractedBlock(EInteractionBlock blockType, InteractionBlock blockBase) {
        this(blockType, blockBase, 0, null, null);
    }

    public InteractedBlock(
            EInteractionBlock blockType, InteractionBlock blockBase,
            double transformationChance, EInteractionBlock transformedType
    ) {
        this(blockType, blockBase, transformationChance, transformedType, null);
    }

    public InteractedBlock(
            EInteractionBlock blockType, InteractionBlock blockBase,
            double transformationChance,
            EInteractionBlock transformedType, InteractionBlock transformedBase
    ) {
        this.blockType = blockType;

        if (blockType.equals(EInteractionBlock.AIR)) {
            this.blockBase = null;
            this.transformationChance = 0;
            this.transformedType = null;
            this.transformedBase = null;
        }
        else {
            if (blockBase == null) throw new IllegalArgumentException("Missing block_base for given Interaction Type");
            if (blockType.equals(EInteractionBlock.BLOCK) && !blockBase.isBlock()) throw new IllegalArgumentException("Wrong block_base for given Interaction Type");
            if (blockType.equals(EInteractionBlock.FLUID) && blockBase.isBlock()) throw new IllegalArgumentException("Wrong block_base for given Interaction Type");
            this.blockBase = blockBase;

            if (transformationChance < 0) transformationChance = 0.;
            else if (transformationChance > 1) transformationChance = 1.;
            this.transformationChance = transformationChance;

            if (transformationChance == 0) {
                this.transformedType = null;
                this.transformedBase = null;
            }
            else {
                this.transformedType = transformedType;

                if (transformedType.equals(EInteractionBlock.AIR)) {
                    this.transformedBase = null;
                }
                else {
                    if (transformedBase == null) throw new IllegalArgumentException("Missing transformed_base for given Interaction Type");
                    if (transformedType.equals(EInteractionBlock.BLOCK) && !blockBase.isBlock()) throw new IllegalArgumentException("Wrong transformed_base for given Interaction Type");
                    if (transformedType.equals(EInteractionBlock.FLUID) && blockBase.isBlock()) throw new IllegalArgumentException("Wrong transformed_base for given Interaction Type");
                    this.transformedBase = transformedBase;
                }
            }
        }
    }

    // Interface ( Getters )

    public EInteractionBlock getBlockType() {
        return blockType;
    }

    public InteractionBlock getBlockBase() {
        return blockBase;
    }

    public double getTransformationChance() {
        return transformationChance;
    }

    public EInteractionBlock getTransformedType() {
        return transformedType;
    }

    public InteractionBlock getTransformedBase() {
        return transformedBase;
    }

    // Interface ( Others )

    public boolean isAir() {
        return this.blockType.equals(EInteractionBlock.AIR);
    }

    public boolean isTransformedAir() {
        return this.transformedType.equals(EInteractionBlock.AIR);
    }

    public boolean canTransform() {
        return this.transformationChance > 0;
    }

    @Override
    public String toString() {
        return "InteractedBlock{" +
                "\nblockType=" + blockType +
                ", \nblockBase=" + blockBase +
                ", \ntransformationChance=" + transformationChance +
                ", \ntransformedType=" + transformedType +
                ", \ntransformedBase=" + transformedBase +
                '}';
    }
}



















