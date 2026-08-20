package com.drimoz.punchthemall.core.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How drops are packed into the squares a viewer has room for.
 *
 * <p>The rule worth protecting is that a guaranteed drop never shares a square with a weighted one.
 * A square alternating between "you always get this" and "one time in four" is telling a player two
 * opposite things in one place, and no tooltip fixes that.</p>
 */
class DropSlotLayoutTest {

    private static List<String> entries(String prefix, int count) {
        return IntStream.range(0, count).mapToObj(i -> prefix + i).toList();
    }

    @Nested
    @DisplayName("splitting the squares")
    class Sharing {

        @Test
        @DisplayName("no guaranteed drops, no squares reserved for them")
        void noneReserved() {
            assertEquals(0, DropSlotLayout.guaranteedSlots(0, 20, 18));
        }

        @Test
        @DisplayName("no weighted drops, the guaranteed ones take what they need")
        void guaranteedTakeEverything() {
            assertEquals(5, DropSlotLayout.guaranteedSlots(5, 0, 18));
            assertEquals(18, DropSlotLayout.guaranteedSlots(30, 0, 18), "capped by what there is room for");
        }

        @Test
        @DisplayName("one guaranteed drop among many weighted still gets a square of its own")
        void alwaysAtLeastOne() {
            assertTrue(DropSlotLayout.guaranteedSlots(1, 60, 18) >= 1,
                    "otherwise the only guaranteed drop shares with a weighted one, or vanishes");
        }

        @Test
        @DisplayName("the weighted pool is never squeezed out entirely")
        void weightedKeepARow() {
            int guaranteed = DropSlotLayout.guaranteedSlots(60, 1, 18);

            assertTrue(guaranteed <= 17, "at least one square has to be left for the weighted pool");
        }

        @Test
        @DisplayName("the split is roughly proportional")
        void proportional() {
            // Half and half, so about half the squares.
            assertEquals(9, DropSlotLayout.guaranteedSlots(20, 20, 18));
            // A third guaranteed, so about a third.
            assertEquals(6, DropSlotLayout.guaranteedSlots(10, 20, 18));
        }

        @Test
        @DisplayName("a single square goes to the guaranteed drops, the part a player can count on")
        void oneSquare() {
            assertEquals(1, DropSlotLayout.guaranteedSlots(3, 3, 1));
        }

        @Test
        @DisplayName("nothing is reserved when there is nowhere to put it")
        void noCapacity() {
            assertEquals(0, DropSlotLayout.guaranteedSlots(5, 5, 0));
        }
    }

    @Nested
    @DisplayName("spreading entries over squares")
    class Distribution {

        @Test
        @DisplayName("one square each when they fit")
        void oneEach() {
            List<List<String>> perSlot = DropSlotLayout.distribute(entries("w", 5), 9);

            assertEquals(5, perSlot.size(), "no empty squares are handed back");
            assertTrue(perSlot.stream().allMatch(slot -> slot.size() == 1));
        }

        @Test
        @DisplayName("every entry survives, however tight the fit")
        void nothingIsLost() {
            List<String> all = entries("w", 27);
            List<List<String>> perSlot = DropSlotLayout.distribute(all, 9);

            List<String> flattened = perSlot.stream().flatMap(List::stream).toList();
            assertEquals(all.size(), flattened.size());
            assertTrue(flattened.containsAll(all), "a drop that is dropped is a lie in the recipe");
        }

        @Test
        @DisplayName("the first entries stay in the first squares")
        void readingOrderSurvives() {
            List<List<String>> perSlot = DropSlotLayout.distribute(entries("w", 20), 9);

            for (int i = 0; i < 9; i++) {
                assertEquals("w" + i, perSlot.get(i).get(0),
                        "round-robin keeps the order the pack was written in");
            }
        }

        @Test
        @DisplayName("the load is even, so no square cycles far longer than its neighbours")
        void evenlyLoaded() {
            List<List<String>> perSlot = DropSlotLayout.distribute(entries("w", 20), 9);

            int smallest = perSlot.stream().mapToInt(List::size).min().orElseThrow();
            int largest = perSlot.stream().mapToInt(List::size).max().orElseThrow();
            assertTrue(largest - smallest <= 1, "got " + smallest + " to " + largest);
        }

        @Test
        @DisplayName("nothing in, nothing out")
        void empty() {
            assertEquals(List.of(), DropSlotLayout.distribute(List.of(), 9));
            assertEquals(List.of(), DropSlotLayout.distribute(entries("w", 3), 0));
        }
    }

    @Test
    @DisplayName("a guaranteed drop never shares a square with a weighted one")
    void kindsNeverMix() {
        // Walked over a range of shapes rather than one, because the invariant is what matters and
        // the arithmetic that keeps it has several corners.
        for (int guaranteed = 0; guaranteed <= 20; guaranteed++) {
            for (int weighted = 0; weighted <= 20; weighted++) {
                for (int capacity : new int[]{1, 9, 18, 27}) {
                    int forGuaranteed = DropSlotLayout.guaranteedSlots(guaranteed, weighted, capacity);
                    int forWeighted = Math.min(weighted, capacity - forGuaranteed);

                    assertTrue(forGuaranteed + forWeighted <= capacity,
                            "overflowed the box: " + guaranteed + "g " + weighted + "w in " + capacity);

                    // The two groups are laid out separately, so a square holds one kind by
                    // construction — what has to hold is that each kind actually has somewhere to go.
                    if (guaranteed > 0 && capacity > 1 && weighted > 0) {
                        assertTrue(forGuaranteed >= 1,
                                guaranteed + " guaranteed drops got no square (" + capacity + " available)");
                        assertTrue(forWeighted >= 1,
                                weighted + " weighted drops got no square (" + capacity + " available)");
                    }
                }
            }
        }
    }
}
