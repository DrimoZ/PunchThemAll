package com.drimoz.punchthemall.core.model.enums;

import java.util.Locale;

/**
 * What a transformation does to the block it lands on.
 *
 * <p>{@link #REPLACE} is the only behaviour the mod had before offsets existed, so it stays the
 * default and every pre-existing file keeps working untouched.</p>
 */
public enum PtaTransformOp {

    /** Overwrite whatever is there. No drops, no questions asked. */
    REPLACE,

    /**
     * Destroy the block that is there, the way a player would — block-break particles, break sound
     * and (unless the transformation opts out) its loot. Nothing is written in its place.
     */
    BREAK,

    /** Write a block only where there is room for one; an occupied destination is left alone. */
    PLACE;

    public static PtaTransformOp fromString(String op) {
        for (PtaTransformOp value : values()) {
            if (value.name().equalsIgnoreCase(op)) return value;
        }
        throw new IllegalArgumentException("Unknown transformation op: " + op);
    }

    /** Whether this op needs an {@code into} block/fluid to write. {@link #BREAK} does not. */
    public boolean needsTarget() {
        return this != BREAK;
    }

    public String serialized() {
        return name().toLowerCase(Locale.ROOT);
    }
}
