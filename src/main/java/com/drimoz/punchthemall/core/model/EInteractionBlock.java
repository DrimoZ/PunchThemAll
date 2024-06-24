package com.drimoz.punchthemall.core.model;

public enum EInteractionBlock {
    BLOCK,
    FLUID,
    AIR;

    public static EInteractionBlock fromString(String type) {
        for (EInteractionBlock interactionType : EInteractionBlock.values()) {
            if (interactionType.name().equalsIgnoreCase(type)) {
                return interactionType;
            }
        }
        throw new IllegalArgumentException("Unknown interaction type: " + type);
    }
}
