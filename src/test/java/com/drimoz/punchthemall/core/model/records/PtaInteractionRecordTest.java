package com.drimoz.punchthemall.core.model.records;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A player cost (damage or hunger): its chance, and the amount it takes. */
class PtaInteractionRecordTest {

    @Test
    @DisplayName("chance is clamped to [0, 1]")
    void chanceClamped() {
        assertEquals(0.0, new PtaInteractionRecord(-3, 1, 1).chance());
        assertEquals(1.0, new PtaInteractionRecord(7, 1, 1).chance());
        assertEquals(0.5, new PtaInteractionRecord(0.5, 1, 1).chance());
    }

    @Test
    @DisplayName("a cost always takes at least one point")
    void minimumIsOne() {
        assertEquals(1, new PtaInteractionRecord(1, 0, 0).min());
        assertEquals(1, new PtaInteractionRecord(1, -5, -5).min());
    }

    @Test
    @DisplayName("max is clamped against the floored min, not the raw one")
    void maxClampedAgainstFlooredMin() {
        // With `Math.max(min, max)` on the raw parameter, a (0, 0) cost produced min=1, max=0 — an
        // inverted range that getValue() only papered over by accident.
        PtaInteractionRecord record = new PtaInteractionRecord(1, 0, 0);
        assertEquals(1, record.min());
        assertEquals(1, record.max());
        assertTrue(record.max() >= record.min());

        PtaInteractionRecord negative = new PtaInteractionRecord(1, -5, -1);
        assertTrue(negative.max() >= negative.min());
    }

    @Test
    @DisplayName("a chance of 0 never fires and a chance of 1 always does")
    void chanceExtremes() {
        RandomSource random = RandomSource.create(42L);

        PtaInteractionRecord always = new PtaInteractionRecord(1, 1, 1);
        PtaInteractionRecord never = new PtaInteractionRecord(0, 1, 1);

        for (int i = 0; i < 500; i++) {
            assertTrue(always.shouldExecute(random));
        }
        // nextDouble() is in [0, 1), so only an exact 0.0 could slip through a zero chance; over this
        // many draws that is not going to happen, and a failure here would be a real regression.
        int fired = 0;
        for (int i = 0; i < 500; i++) {
            if (never.shouldExecute(random)) fired++;
        }
        assertEquals(0, fired);
    }

    @Test
    @DisplayName("the rolled amount stays within the range and reaches both ends")
    void valueWithinRange() {
        PtaInteractionRecord record = new PtaInteractionRecord(1, 2, 5);
        RandomSource random = RandomSource.create(7L);

        boolean sawMin = false;
        boolean sawMax = false;
        for (int i = 0; i < 500; i++) {
            int value = record.getValue(random);
            assertTrue(value >= 2 && value <= 5, "value out of range: " + value);
            sawMin |= value == 2;
            sawMax |= value == 5;
        }

        assertTrue(sawMin && sawMax);
    }

    @Test
    @DisplayName("an exact cost is constant")
    void exactValue() {
        PtaInteractionRecord record = new PtaInteractionRecord(1, 3, 3);
        RandomSource random = RandomSource.create(7L);

        for (int i = 0; i < 50; i++) {
            assertEquals(3, record.getValue(random));
        }
    }

    @Test
    @DisplayName("a mid chance fires sometimes and not always")
    void midChance() {
        PtaInteractionRecord record = new PtaInteractionRecord(0.5, 1, 1);
        RandomSource random = RandomSource.create(99L);

        int fired = 0;
        for (int i = 0; i < 2000; i++) {
            if (record.shouldExecute(random)) fired++;
        }

        assertTrue(fired > 800 && fired < 1200, "expected roughly half of 2000, got " + fired);
        assertFalse(fired == 0 || fired == 2000);
    }
}
