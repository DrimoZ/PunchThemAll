package com.drimoz.punchthemall.core.model;

public enum EInteractionHand {
    ANY_HAND("any"),
    MAIN_HAND("main"),
    OFF_HAND("off");

    private final String value;

    EInteractionHand(String value) {
        this.value = value;
    }

    public static EInteractionHand fromValueOrName(String type) {
        for (EInteractionHand interactionType : EInteractionHand.values()) {
            if (interactionType.value.equalsIgnoreCase(type) || interactionType.name().equalsIgnoreCase(type)) {
                return interactionType;
            }
        }
        throw new IllegalArgumentException("Unknown interaction type: " + type);
    }

    public static EInteractionHand fromValue(String type) {
        for (EInteractionHand interactionType : EInteractionHand.values()) {
            if (interactionType.value.equalsIgnoreCase(type)) {
                return interactionType;
            }
        }
        throw new IllegalArgumentException("Unknown interaction type: " + type);
    }

    public static EInteractionHand fromName(String type) {
        for (EInteractionHand interactionType : EInteractionHand.values()) {
            if (interactionType.name().equalsIgnoreCase(type)) {
                return interactionType;
            }
        }
        throw new IllegalArgumentException("Unknown interaction type: " + type);
    }
}
