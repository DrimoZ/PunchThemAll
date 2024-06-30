package com.drimoz.punchthemall.core.model.enums;

public enum PtaHandEnum {
    ANY_HAND("any"),
    MAIN_HAND("main"),
    OFF_HAND("off");

    private final String value;

    PtaHandEnum(String value) {
        this.value = value;
    }

    public static PtaHandEnum fromValueOrName(String type) {
        for (PtaHandEnum interactionType : PtaHandEnum.values()) {
            if (interactionType.value.equalsIgnoreCase(type) || interactionType.name().equalsIgnoreCase(type)) {
                return interactionType;
            }
        }
        throw new IllegalArgumentException("Unknown interaction type: " + type);
    }

    public static PtaHandEnum fromValue(String type) {
        for (PtaHandEnum interactionType : PtaHandEnum.values()) {
            if (interactionType.value.equalsIgnoreCase(type)) {
                return interactionType;
            }
        }
        throw new IllegalArgumentException("Unknown interaction type: " + type);
    }

    public static PtaHandEnum fromName(String type) {
        for (PtaHandEnum interactionType : PtaHandEnum.values()) {
            if (interactionType.name().equalsIgnoreCase(type)) {
                return interactionType;
            }
        }
        throw new IllegalArgumentException("Unknown interaction type: " + type);
    }
}
