package com.drimoz.punchthemall.core.model;

public enum EInteractionType {
    RIGHT_CLICK,
    SHIFT_RIGHT_CLICK,
    LEFT_CLICK,
    SHIFT_LEFT_CLICK;

    public static EInteractionType fromString(String type) {
        for (EInteractionType interactionType : EInteractionType.values()) {
            if (interactionType.name().equalsIgnoreCase(type)) {
                return interactionType;
            }
        }
        throw new IllegalArgumentException("Unknown interaction type: " + type);
    }
}
