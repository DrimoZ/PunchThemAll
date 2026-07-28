package com.drimoz.punchthemall.core.model.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The two authored enums: click type and hand. */
class PtaEnumsTest {

    @Nested
    @DisplayName("PtaTypeEnum")
    class Types {

        @Test
        @DisplayName("every name parses, case-insensitively")
        void fromString() {
            assertEquals(PtaTypeEnum.RIGHT_CLICK, PtaTypeEnum.fromString("right_click"));
            assertEquals(PtaTypeEnum.RIGHT_CLICK, PtaTypeEnum.fromString("RIGHT_CLICK"));
            assertEquals(PtaTypeEnum.SHIFT_LEFT_CLICK, PtaTypeEnum.fromString("Shift_Left_Click"));
        }

        @Test
        @DisplayName("an unknown type is rejected rather than defaulted")
        void unknownRejected() {
            assertThrows(IllegalArgumentException.class, () -> PtaTypeEnum.fromString("middle_click"));
            assertThrows(IllegalArgumentException.class, () -> PtaTypeEnum.fromString(""));
        }

        @Test
        @DisplayName("sneaking promotes a click to its shift variant")
        void sneakPromotes() {
            assertEquals(PtaTypeEnum.RIGHT_CLICK, PtaTypeEnum.getTypeFromEvent(PtaTypeEnum.RIGHT_CLICK, false));
            assertEquals(PtaTypeEnum.SHIFT_RIGHT_CLICK, PtaTypeEnum.getTypeFromEvent(PtaTypeEnum.RIGHT_CLICK, true));
            assertEquals(PtaTypeEnum.LEFT_CLICK, PtaTypeEnum.getTypeFromEvent(PtaTypeEnum.LEFT_CLICK, false));
            assertEquals(PtaTypeEnum.SHIFT_LEFT_CLICK, PtaTypeEnum.getTypeFromEvent(PtaTypeEnum.LEFT_CLICK, true));
        }

        @Test
        @DisplayName("an already-shifted type is not a valid event type")
        void shiftedTypeIsNotAnEvent() {
            // Events only ever report a plain click; the shift variants are derived here, so passing
            // one back in means a caller has resolved twice.
            assertThrows(IllegalArgumentException.class,
                    () -> PtaTypeEnum.getTypeFromEvent(PtaTypeEnum.SHIFT_LEFT_CLICK, true));
        }

        @Test
        @DisplayName("isLeftClick and isShiftClick classify all four")
        void classification() {
            assertTrue(PtaTypeEnum.LEFT_CLICK.isLeftClick());
            assertTrue(PtaTypeEnum.SHIFT_LEFT_CLICK.isLeftClick());
            assertFalse(PtaTypeEnum.RIGHT_CLICK.isLeftClick());
            assertFalse(PtaTypeEnum.SHIFT_RIGHT_CLICK.isLeftClick());

            assertTrue(PtaTypeEnum.SHIFT_LEFT_CLICK.isShiftClick());
            assertTrue(PtaTypeEnum.SHIFT_RIGHT_CLICK.isShiftClick());
            assertFalse(PtaTypeEnum.LEFT_CLICK.isShiftClick());
            assertFalse(PtaTypeEnum.RIGHT_CLICK.isShiftClick());
        }
    }

    @Nested
    @DisplayName("PtaHandEnum")
    class Hands {

        @Test
        @DisplayName("both the short value and the enum name parse")
        void fromValueOrName() {
            assertEquals(PtaHandEnum.ANY_HAND, PtaHandEnum.fromValueOrName("any"));
            assertEquals(PtaHandEnum.ANY_HAND, PtaHandEnum.fromValueOrName("ANY_HAND"));
            assertEquals(PtaHandEnum.MAIN_HAND, PtaHandEnum.fromValueOrName("main"));
            assertEquals(PtaHandEnum.OFF_HAND, PtaHandEnum.fromValueOrName("OFF"));
        }

        @Test
        @DisplayName("the value-only and name-only forms are stricter")
        void strictForms() {
            assertEquals(PtaHandEnum.MAIN_HAND, PtaHandEnum.fromValue("main"));
            assertThrows(IllegalArgumentException.class, () -> PtaHandEnum.fromValue("MAIN_HAND"));

            assertEquals(PtaHandEnum.MAIN_HAND, PtaHandEnum.fromName("main_hand"));
            assertThrows(IllegalArgumentException.class, () -> PtaHandEnum.fromName("main"));
        }

        @Test
        @DisplayName("an unknown hand is rejected")
        void unknownRejected() {
            assertThrows(IllegalArgumentException.class, () -> PtaHandEnum.fromValueOrName("third"));
        }
    }
}
