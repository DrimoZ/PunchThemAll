package com.drimoz.punchthemall.core.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The SNBT whitelist/blacklist matcher behind {@code hand.nbt} and {@code target.nbt}. */
class TagHelperTest {

    private static CompoundTag compound(String key, net.minecraft.nbt.Tag value) {
        CompoundTag tag = new CompoundTag();
        tag.put(key, value);
        return tag;
    }

    private static CompoundTag rangeTag(long min, long max) {
        ListTag range = new ListTag();
        range.add(IntTag.valueOf((int) min));
        range.add(IntTag.valueOf((int) max));
        return compound("RangeTag", range);
    }

    @Nested
    @DisplayName("whitelist")
    class Whitelist {

        @Test
        @DisplayName("an empty or absent matcher accepts anything")
        void emptyMatcherPasses() {
            assertTrue(TagHelper.containsRequiredTagsWithRange(new CompoundTag(), null));
            assertTrue(TagHelper.containsRequiredTagsWithRange(new CompoundTag(), new CompoundTag()));
            assertTrue(TagHelper.containsRequiredTagsWithRange(new CompoundTag(), new ListTag()));
        }

        @Test
        @DisplayName("a required key must be present and equal")
        void requiresMatchingScalar() {
            CompoundTag item = compound("Damage", IntTag.valueOf(5));

            assertTrue(TagHelper.containsRequiredTagsWithRange(item, compound("Damage", IntTag.valueOf(5))));
            assertFalse(TagHelper.containsRequiredTagsWithRange(item, compound("Damage", IntTag.valueOf(6))));
            assertFalse(TagHelper.containsRequiredTagsWithRange(item, compound("Missing", IntTag.valueOf(5))));
        }

        @Test
        @DisplayName("extra keys on the item are ignored")
        void extraItemKeysIgnored() {
            CompoundTag item = new CompoundTag();
            item.putInt("Damage", 5);
            item.putString("Unrelated", "x");

            assertTrue(TagHelper.containsRequiredTagsWithRange(item, compound("Damage", IntTag.valueOf(5))));
        }

        @Test
        @DisplayName("RangeTag matches inclusively on both bounds")
        void rangeIsInclusive() {
            CompoundTag matcher = compound("Damage", rangeTag(2, 7));

            assertTrue(TagHelper.containsRequiredTagsWithRange(compound("Damage", IntTag.valueOf(2)), matcher));
            assertTrue(TagHelper.containsRequiredTagsWithRange(compound("Damage", IntTag.valueOf(7)), matcher));
            assertTrue(TagHelper.containsRequiredTagsWithRange(compound("Damage", IntTag.valueOf(4)), matcher));
            assertFalse(TagHelper.containsRequiredTagsWithRange(compound("Damage", IntTag.valueOf(1)), matcher));
            assertFalse(TagHelper.containsRequiredTagsWithRange(compound("Damage", IntTag.valueOf(8)), matcher));
        }

        @Test
        @DisplayName("RangeTag compares numerically across tag widths")
        void rangeIgnoresNumericWidth() {
            // Authored SNBT freely mixes `[0,500]` (int) and `3s` (short); the value read off the item
            // need not use the same width, and matching on the exact tag type used to reject these.
            CompoundTag item = compound("lvl", ShortTag.valueOf((short) 3));
            assertTrue(TagHelper.containsRequiredTagsWithRange(item, compound("lvl", rangeTag(1, 5))));
        }

        @Test
        @DisplayName("RangeTag rejects a non-numeric value instead of throwing")
        void rangeAgainstNonNumeric() {
            CompoundTag item = compound("lvl", StringTag.valueOf("three"));
            assertFalse(TagHelper.containsRequiredTagsWithRange(item, compound("lvl", rangeTag(1, 5))));
        }

        @Test
        @DisplayName("every required list element must find a match, in any order")
        void listRequiresEachElement() {
            ListTag itemList = new ListTag();
            itemList.add(compound("id", StringTag.valueOf("minecraft:fortune")));
            itemList.add(compound("id", StringTag.valueOf("minecraft:unbreaking")));

            ListTag wanted = new ListTag();
            wanted.add(compound("id", StringTag.valueOf("minecraft:unbreaking")));

            assertTrue(TagHelper.containsRequiredTagsWithRange(compound("Enchantments", itemList),
                    compound("Enchantments", wanted)));

            ListTag missing = new ListTag();
            missing.add(compound("id", StringTag.valueOf("minecraft:silk_touch")));

            assertFalse(TagHelper.containsRequiredTagsWithRange(compound("Enchantments", itemList),
                    compound("Enchantments", missing)));
        }

        @Test
        @DisplayName("a list matcher against a non-list value fails rather than throwing")
        void listAgainstNonList() {
            // Regression: the list branch used to cast the item value to ListTag unchecked, so a
            // shape mismatch (another mod's custom_data, a block entity that changed layout) threw
            // out of the interaction filter — that is, mid-click.
            ListTag wanted = new ListTag();
            wanted.add(IntTag.valueOf(1));
            CompoundTag matcher = compound("values", wanted);
            CompoundTag item = compound("values", StringTag.valueOf("not a list"));

            assertDoesNotThrow(() -> TagHelper.containsRequiredTagsWithRange(item, matcher));
            assertFalse(TagHelper.containsRequiredTagsWithRange(item, matcher));
        }

        @Test
        @DisplayName("a compound matcher against a scalar fails")
        void compoundAgainstScalar() {
            CompoundTag matcher = compound("custom", compound("tier", IntTag.valueOf(2)));
            CompoundTag item = compound("custom", IntTag.valueOf(2));

            assertFalse(TagHelper.containsRequiredTagsWithRange(item, matcher));
        }

        @Test
        @DisplayName("types must agree for scalar equality")
        void scalarTypesMustAgree() {
            // A short 5 and an int 5 are different tags outside a RangeTag, and always have been.
            assertFalse(TagHelper.containsRequiredTagsWithRange(
                    compound("v", ShortTag.valueOf((short) 5)), compound("v", IntTag.valueOf(5))));
        }

        @Test
        @DisplayName("nested compounds recurse")
        void nestedCompounds() {
            CompoundTag inner = new CompoundTag();
            inner.putInt("tier", 3);
            inner.putString("owner", "theo");

            CompoundTag item = compound("custom", inner);

            assertTrue(TagHelper.containsRequiredTagsWithRange(item, compound("custom", compound("tier", IntTag.valueOf(3)))));
            assertFalse(TagHelper.containsRequiredTagsWithRange(item, compound("custom", compound("tier", IntTag.valueOf(4)))));
        }
    }

    @Nested
    @DisplayName("blacklist")
    class Blacklist {

        @Test
        @DisplayName("an empty matcher forbids nothing")
        void emptyMatcherPasses() {
            assertTrue(TagHelper.containsRequiredTagsWithRangeBlacklist(new CompoundTag(), null));
            assertTrue(TagHelper.containsRequiredTagsWithRangeBlacklist(new CompoundTag(), new CompoundTag()));
        }

        @Test
        @DisplayName("a forbidden value fails, a different one passes")
        void forbidsMatchingScalar() {
            CompoundTag matcher = compound("Damage", IntTag.valueOf(5));

            assertFalse(TagHelper.containsRequiredTagsWithRangeBlacklist(compound("Damage", IntTag.valueOf(5)), matcher));
            assertTrue(TagHelper.containsRequiredTagsWithRangeBlacklist(compound("Damage", IntTag.valueOf(6)), matcher));
        }

        @Test
        @DisplayName("an absent key cannot be forbidden")
        void absentKeyPasses() {
            assertTrue(TagHelper.containsRequiredTagsWithRangeBlacklist(
                    new CompoundTag(), compound("Damage", IntTag.valueOf(5))));
        }

        @Test
        @DisplayName("RangeTag forbids values inside [min, max] and only those")
        void rangeExcludesInterval() {
            CompoundTag matcher = compound("Damage", rangeTag(2, 7));

            assertFalse(TagHelper.containsRequiredTagsWithRangeBlacklist(compound("Damage", IntTag.valueOf(2)), matcher));
            assertFalse(TagHelper.containsRequiredTagsWithRangeBlacklist(compound("Damage", IntTag.valueOf(7)), matcher));
            assertFalse(TagHelper.containsRequiredTagsWithRangeBlacklist(compound("Damage", IntTag.valueOf(5)), matcher));
            assertTrue(TagHelper.containsRequiredTagsWithRangeBlacklist(compound("Damage", IntTag.valueOf(1)), matcher));
            assertTrue(TagHelper.containsRequiredTagsWithRangeBlacklist(compound("Damage", IntTag.valueOf(8)), matcher));
        }

        @Test
        @DisplayName("a non-numeric value cannot fall inside a forbidden range")
        void rangeAgainstNonNumericPasses() {
            assertTrue(TagHelper.containsRequiredTagsWithRangeBlacklist(
                    compound("lvl", StringTag.valueOf("three")), compound("lvl", rangeTag(1, 5))));
        }

        @Test
        @DisplayName("a list matcher against a non-list value passes rather than throwing")
        void listAgainstNonList() {
            ListTag forbidden = new ListTag();
            forbidden.add(IntTag.valueOf(1));
            CompoundTag matcher = compound("values", forbidden);
            CompoundTag item = compound("values", StringTag.valueOf("not a list"));

            assertDoesNotThrow(() -> TagHelper.containsRequiredTagsWithRangeBlacklist(item, matcher));
            assertTrue(TagHelper.containsRequiredTagsWithRangeBlacklist(item, matcher));
        }

        @Test
        @DisplayName("a forbidden enchantment in a list fails")
        void forbidsListElement() {
            ListTag itemList = new ListTag();
            itemList.add(compound("id", StringTag.valueOf("minecraft:silk_touch")));

            ListTag forbidden = new ListTag();
            forbidden.add(compound("id", StringTag.valueOf("minecraft:silk_touch")));

            assertFalse(TagHelper.containsRequiredTagsWithRangeBlacklist(
                    compound("Enchantments", itemList), compound("Enchantments", forbidden)));
        }
    }
}
