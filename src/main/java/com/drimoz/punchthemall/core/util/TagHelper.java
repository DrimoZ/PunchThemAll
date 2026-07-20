package com.drimoz.punchthemall.core.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;

import java.util.Set;

public class TagHelper {
    private TagHelper() {}

    public static boolean containsRequiredTagsWithRange(Object itemTag, Object compareTag) {
        // Nothing to compare against -> pass.
        if (compareTag == null || (compareTag instanceof ListTag && ((ListTag) compareTag).isEmpty()) ||
                (compareTag instanceof CompoundTag && ((CompoundTag) compareTag).isEmpty())) return true;

        if (compareTag instanceof CompoundTag) {
            Set<String> compareKeys = ((CompoundTag) compareTag).getAllKeys();

            // Special-case the RangeTag convention. Compare numerically rather than by tag class:
            // authored SNBT freely mixes `[0,500]` (int) and `[2s,7s]` (short), and the value read
            // from the item need not use the same width, so matching on the exact tag type made
            // valid files silently fail to match — or throw when the two widths disagreed.
            if (compareKeys.size() == 1 && compareKeys.contains("RangeTag")) {
                if (!(itemTag instanceof NumericTag itemNumeric)) return false;
                if (!(((CompoundTag) compareTag).get("RangeTag") instanceof ListTag listRangeTag) || listRangeTag.size() != 2) return false;
                if (!(listRangeTag.get(0) instanceof NumericTag minTag) || !(listRangeTag.get(1) instanceof NumericTag maxTag)) return false;

                long value = itemNumeric.getAsLong();
                return minTag.getAsLong() <= value && value <= maxTag.getAsLong();
            }
            // Otherwise recurse on each field.
            else {
                if (!(itemTag instanceof CompoundTag)) return false;

                Set<String> itemKeys = ((CompoundTag) itemTag).getAllKeys();
                for (String compareKey : compareKeys) {
                    if (!itemKeys.contains(compareKey)) return false;

                    if (!containsRequiredTagsWithRange(
                            ((CompoundTag) itemTag).get(compareKey),
                            ((CompoundTag) compareTag).get(compareKey)))
                        return false;
                }

                return true;
            }
        }
        else if (compareTag instanceof ListTag) {
            for (var compareVal : ((ListTag) compareTag).stream().toList()) {
                boolean test = false;
                for (var itemVal : ((ListTag) itemTag).stream().toList()) {
                    if (containsRequiredTagsWithRange(itemVal, compareVal)) {
                        test = true;
                        break;
                    }
                }
                if (!test) return false;
            }

            return true;
        }
        else {
            return compareTag.getClass().equals(itemTag.getClass()) && compareTag.equals(itemTag);
        }
    }

    public static boolean containsRequiredTagsWithRangeBlacklist(Object itemTag, Object compareTag) {
        if (compareTag == null || (compareTag instanceof ListTag && ((ListTag) compareTag).isEmpty()) ||
                (compareTag instanceof CompoundTag && ((CompoundTag) compareTag).isEmpty())) return true;

        if (compareTag instanceof CompoundTag) {
            Set<String> compareKeys = ((CompoundTag) compareTag).getAllKeys();

            if (compareKeys.size() == 1 && compareKeys.contains("RangeTag")) {
                // A RangeTag in a blacklist forbids values inside [min, max]; everything else passes.
                if (!(itemTag instanceof NumericTag itemNumeric)) return true;

                if (!(((CompoundTag) compareTag).get("RangeTag") instanceof ListTag listRangeTag)
                        || listRangeTag.size() != 2
                        || !(listRangeTag.get(0) instanceof NumericTag minTag)
                        || !(listRangeTag.get(1) instanceof NumericTag maxTag)) {
                    return true;
                }

                long value = itemNumeric.getAsLong();
                return value < minTag.getAsLong() || value > maxTag.getAsLong();
            }
            else {
                if (!(itemTag instanceof CompoundTag)) return true;

                Set<String> itemKeys = ((CompoundTag) itemTag).getAllKeys();
                for (String compareKey : compareKeys) {
                    if (itemKeys.contains(compareKey)) {
                        if (!containsRequiredTagsWithRangeBlacklist(
                                ((CompoundTag) itemTag).get(compareKey),
                                ((CompoundTag) compareTag).get(compareKey))) {
                            return false;
                        }
                    }
                }

                return true;
            }
        } else if (compareTag instanceof ListTag) {
            for (var compareVal : ((ListTag) compareTag).stream().toList()) {
                boolean test = true;
                for (var itemVal : ((ListTag) itemTag).stream().toList()) {
                    if (!containsRequiredTagsWithRangeBlacklist(itemVal, compareVal)) {
                        test = false;
                        break;
                    }
                }
                if (!test) return false;
            }

            return true;
        } else {
            return !compareTag.getClass().equals(itemTag.getClass()) || !compareTag.equals(itemTag);
        }
    }
}
