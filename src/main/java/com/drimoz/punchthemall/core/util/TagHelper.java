package com.drimoz.punchthemall.core.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.ShortTag;

import java.util.Set;

public class TagHelper {
    private TagHelper() {}

    public static boolean containsRequiredTagsWithRange(Object itemTag, Object compareTag) {
        // Nothing to compare against -> pass.
        if (compareTag == null || (compareTag instanceof ListTag && ((ListTag) compareTag).isEmpty()) ||
                (compareTag instanceof CompoundTag && ((CompoundTag) compareTag).isEmpty())) return true;

        if (compareTag instanceof CompoundTag) {
            Set<String> compareKeys = ((CompoundTag) compareTag).getAllKeys();

            // Special-case the RangeTag convention.
            if (compareKeys.size() == 1 && compareKeys.contains("RangeTag")) {
                if (itemTag instanceof IntTag) {
                    if (!(((CompoundTag) compareTag).get("RangeTag") instanceof ListTag listRangeTag) || listRangeTag.size() != 2) return false;
                    return ((IntTag) listRangeTag.get(0)).getAsInt() <= ((IntTag) itemTag).getAsInt() && ((IntTag) itemTag).getAsInt() <= ((IntTag) listRangeTag.get(1)).getAsInt();
                }
                if (itemTag instanceof ShortTag) {
                    if (!(((CompoundTag) compareTag).get("RangeTag") instanceof ListTag listRangeTag) || listRangeTag.size() != 2) return false;
                    return ((ShortTag) listRangeTag.get(0)).getAsInt() <= ((ShortTag) itemTag).getAsInt() && ((ShortTag) itemTag).getAsInt() <= ((ShortTag) listRangeTag.get(1)).getAsInt();
                }
                return false;
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
                int value;
                if (itemTag instanceof IntTag intTag) value = intTag.getAsInt();
                else if (itemTag instanceof ShortTag shortTag) value = shortTag.getAsInt();
                else return true;

                if (!(((CompoundTag) compareTag).get("RangeTag") instanceof ListTag listRangeTag)
                        || listRangeTag.size() != 2
                        || !(listRangeTag.get(0) instanceof NumericTag minTag)
                        || !(listRangeTag.get(1) instanceof NumericTag maxTag)) {
                    return true;
                }

                return value < minTag.getAsInt() || value > maxTag.getAsInt();
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
