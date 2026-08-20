package com.drimoz.punchthemall.core.model.enums;

import java.util.Locale;

/** What a {@link PtaTransformOp#BREAK} leaves behind. */
public enum PtaDropMode {

    /** Nothing at all. */
    NONE,

    /** The block's own loot, as if broken by a bare hand. The default. */
    VANILLA,

    /**
     * The block's loot as broken by whatever the player is holding, so Fortune and Silk Touch apply.
     *
     * <p>Off by default on purpose: it lets an interaction mine at a distance with an enchanted tool,
     * which is a fine thing for a pack to choose and a poor thing to inherit by accident.</p>
     */
    TOOL;

    public static PtaDropMode fromString(String mode) {
        for (PtaDropMode value : values()) {
            if (value.name().equalsIgnoreCase(mode)) return value;
        }
        throw new IllegalArgumentException("Unknown drop mode: " + mode);
    }

    public boolean drops() {
        return this != NONE;
    }

    public String serialized() {
        return name().toLowerCase(Locale.ROOT);
    }
}
