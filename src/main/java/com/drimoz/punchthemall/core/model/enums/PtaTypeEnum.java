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

    public boolean isShiftClick() {
        return this == SHIFT_RIGHT_CLICK || this == SHIFT_LEFT_CLICK;
    }
}
