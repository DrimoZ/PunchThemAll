package com.drimoz.punchthemall.core.util;

import java.util.ArrayList;
import java.util.List;

/**
 * How an interaction's drops are packed into the squares a recipe viewer has room for.
 *
 * <p>A viewer sizes a category rather than a recipe, so the number of squares is fixed for every
 * interaction in the pack. When an interaction has more drops than squares, several drops share one
 * and the viewer cycles between them.</p>
 *
 * <p>Kept here, free of any viewer type, because the interesting part is arithmetic over counts and
 * the interesting part is worth testing. The rule it enforces — that a guaranteed drop never shares
 * a square with a weighted one — is a decision about what a player is told, not a drawing detail.</p>
 */
public final class DropSlotLayout {

    private DropSlotLayout() {}

    /**
     * How many of the available squares go to the guaranteed drops.
     *
     * <p>They get their own. A square alternating between "you always get this" and "one time in
     * four" states two opposite things in one place; the tooltip can follow along correctly and the
     * square is still saying both.</p>
     *
     * <p>The split is proportional, with a floor of one square for each kind that has anything in
     * it — so a single guaranteed drop among thirty weighted ones still gets a square to itself,
     * and thirty guaranteed drops never crowd the weighted pool out of view entirely.</p>
     */
    public static int guaranteedSlots(int guaranteed, int weighted, int capacity) {
        if (guaranteed <= 0 || capacity <= 0) return 0;
        if (weighted <= 0) return Math.min(guaranteed, capacity);
        // Nowhere to put both. What a player always gets is the part they can count on, so it wins
        // the last square — the same reason the guaranteed drops are drawn first.
        if (capacity == 1) return 1;

        int fairShare = Math.round((float) capacity * guaranteed / (guaranteed + weighted));
        return Math.clamp(fairShare, 1, Math.min(guaranteed, capacity - 1));
    }

    /**
     * Spread entries over the squares they were given.
     *
     * <p>Round-robin rather than in blocks, so the first drops stay in the first row and an
     * interaction that fits reads in the order it was written.</p>
     */
    public static <T> List<List<T>> distribute(List<T> entries, int slots) {
        if (entries.isEmpty() || slots <= 0) return List.of();

        int used = Math.min(slots, entries.size());
        List<List<T>> perSlot = new ArrayList<>(used);
        for (int i = 0; i < used; i++) perSlot.add(new ArrayList<>());
        for (int i = 0; i < entries.size(); i++) perSlot.get(i % used).add(entries.get(i));

        return perSlot.stream().map(List::copyOf).toList();
    }
}
