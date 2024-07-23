package com.drimoz.punchthemall.core.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.ShortTag;

import java.util.Set;

public class TagHelper {
    private TagHelper() {}

    public static boolean containsRequiredTagsWithRange(Object itemTag, Object compareTag) {
        // Si il n'y a rien à comparer, on est bon
        if (compareTag == null || (compareTag instanceof ListTag && ((ListTag) compareTag).isEmpty()) ||
                (compareTag instanceof CompoundTag && ((CompoundTag) compareTag).isEmpty())) return true;

        if (compareTag instanceof CompoundTag) {
            Set<String> compareKeys = ((CompoundTag) compareTag).getAllKeys();

            // Si on a un format spécifique, on a un comportement spécifique
            if (compareKeys.size() == 1 && compareKeys.contains("RangeTag")) {
                // si on a un 'RangeTag' dans le compare, on veut un nombre (entier?)
                if (itemTag instanceof IntTag) {
                    // On vérifie que la valeur liée à RangeTag soit une list
                    if (!(((CompoundTag) compareTag).get("RangeTag") instanceof ListTag listRangeTag)) return false;

                    assert listRangeTag.size() == 2 : "RangeTag too short";
                    return ((IntTag)listRangeTag.get(0)).getAsInt() <= ((IntTag) itemTag).getAsInt() && ((IntTag) itemTag).getAsInt() <= ((IntTag)listRangeTag.get(1)).getAsInt();
                }
                if (itemTag instanceof ShortTag) {
                    // On vérifie que la valeur liée à RangeTag soit une list
                    if (!(((CompoundTag) compareTag).get("RangeTag") instanceof ListTag listRangeTag)) return false;

                    assert listRangeTag.size() == 2 : "RangeTag too short";
                    return ((ShortTag)listRangeTag.get(0)).getAsInt() <= ((ShortTag) itemTag).getAsInt() && ((ShortTag) itemTag).getAsInt() <= ((ShortTag)listRangeTag.get(1)).getAsInt();
                }
                else {
                    return false;
                }


            }
            // Sinon pour chaque field de l'objet, on vérifie récursivement que l'item a le meme
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
            // Pour chaque element de la liste, on vérifie récursivement qu'il est dans les tags de l'item
            for (var compareVal : ((ListTag)compareTag).stream().toList()) {
                boolean test = false;
                for (var itemVal : ((ListTag)itemTag).stream().toList()) {
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
            // Si on a atteint un primitif, on se contente de vérifier l'équivalence
            return compareTag.getClass().equals(itemTag.getClass()) && compareTag.equals(itemTag);
        }
    }

    public static boolean containsRequiredTagsWithRangeBlacklist(Object itemTag, Object compareTag) {
        // Si il n'y a rien à comparer, on est bon
        if (compareTag == null || (compareTag instanceof ListTag && ((ListTag) compareTag).isEmpty()) ||
                (compareTag instanceof CompoundTag && ((CompoundTag) compareTag).isEmpty())) return true;

        if (compareTag instanceof CompoundTag) {
            Set<String> compareKeys = ((CompoundTag) compareTag).getAllKeys();

            // Si on a un format spécifique, on a un comportement spécifique
            if (compareKeys.size() == 1 && compareKeys.contains("RangeTag")) {
                // si on a un 'RangeTag' dans le compare, on veut un nombre (entier?)
                if (!(itemTag instanceof IntTag)) return true;

                // On vérifie que la valeur liée à RangeTag soit une list
                if (!(((CompoundTag) compareTag).get("RangeTag") instanceof ListTag listRangeTag)) return true;

                assert listRangeTag.size() == 2 : "RangeTag too short";
                return ((IntTag) listRangeTag.get(0)).getAsInt() > ((IntTag) itemTag).getAsInt() &&
                        ((IntTag) itemTag).getAsInt() > ((IntTag) listRangeTag.get(1)).getAsInt();
            }
            // Sinon pour chaque field de l'objet, on vérifie récursivement que l'item n'ai pas les memes
            else {
                if (!(itemTag instanceof CompoundTag)) return true;

                Set<String> itemKeys = ((CompoundTag) itemTag).getAllKeys();
                for (String compareKey : compareKeys) {
                    if (itemKeys.contains(compareKey)) {
                        // Si containsRequiredTagsWithRangeBlacklist = true => Pas d'occurence => on check la clé suivante
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
            // Pour chaque element de la liste, on vérifie récursivement qu'il est dans les tags de l'item
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
            // Si on a atteint un primitif, on se contente de vérifier l'équivalence
            return !compareTag.getClass().equals(itemTag.getClass()) || !compareTag.equals(itemTag);
        }
    }

}