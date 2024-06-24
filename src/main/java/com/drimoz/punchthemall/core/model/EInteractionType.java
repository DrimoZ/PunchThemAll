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

    public static EInteractionType getTypeFromEvent(EInteractionType typeFromEvent, boolean isShiftKeyDown) {
        if (typeFromEvent == EInteractionType.RIGHT_CLICK) {
            return isShiftKeyDown ? EInteractionType.SHIFT_RIGHT_CLICK : EInteractionType.RIGHT_CLICK;
        } else if (typeFromEvent == EInteractionType.LEFT_CLICK) {
            return isShiftKeyDown ? EInteractionType.SHIFT_LEFT_CLICK : EInteractionType.LEFT_CLICK;
        } else {
            throw new IllegalArgumentException("Unexpected value: " + typeFromEvent);
        }
    }
}
