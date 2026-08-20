package com.drimoz.punchthemall.core.model.enums;

public enum PtaTypeEnum {
    RIGHT_CLICK,
    SHIFT_RIGHT_CLICK,
    LEFT_CLICK,
    SHIFT_LEFT_CLICK;

    public static PtaTypeEnum fromString(String type) {
        for (PtaTypeEnum interactionType : PtaTypeEnum.values()) {
            if (interactionType.name().equalsIgnoreCase(type)) {
                return interactionType;
            }
        }
        throw new IllegalArgumentException("Unknown interaction type: " + type);
    }

    public static PtaTypeEnum getTypeFromEvent(PtaTypeEnum typeFromEvent, boolean isShiftKeyDown) {
        if (typeFromEvent == PtaTypeEnum.RIGHT_CLICK) {
            return isShiftKeyDown ? PtaTypeEnum.SHIFT_RIGHT_CLICK : PtaTypeEnum.RIGHT_CLICK;
        } else if (typeFromEvent == PtaTypeEnum.LEFT_CLICK) {
            return isShiftKeyDown ? PtaTypeEnum.SHIFT_LEFT_CLICK : PtaTypeEnum.LEFT_CLICK;
        } else {
            throw new IllegalArgumentException("Unexpected value: " + typeFromEvent);
        }
    }

    public boolean isLeftClick() {
        return this == LEFT_CLICK || this == SHIFT_LEFT_CLICK;
    }

    /**
     * The same click, with sneaking required or not.
     *
     * <p>Sneaking is part of the type, which is why {@code conditions.requires_sneaking} is folded
     * into it when a file uses both: two places saying whether to sneak is two places to
     * disagree, and the disagreement produced an interaction that could never fire.</p>
     */
    public PtaTypeEnum withSneaking(boolean sneaking) {
        if (sneaking == isShiftClick()) return this;
        return switch (this) {
            case LEFT_CLICK -> SHIFT_LEFT_CLICK;
            case SHIFT_LEFT_CLICK -> LEFT_CLICK;
            case RIGHT_CLICK -> SHIFT_RIGHT_CLICK;
            case SHIFT_RIGHT_CLICK -> RIGHT_CLICK;
        };
    }

    public boolean isShiftClick() {
        return this == SHIFT_RIGHT_CLICK || this == SHIFT_LEFT_CLICK;
    }
}
