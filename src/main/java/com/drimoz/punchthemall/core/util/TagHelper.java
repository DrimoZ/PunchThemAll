package com.drimoz.punchthemall.core.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.Map;
import java.util.Set;

public class TagHelper {
    private TagHelper() {}

    public static boolean checkNBTs(CompoundTag itemTag, CompoundTag requiredTag, boolean shouldInclude) {
        if (requiredTag.isEmpty()) return true;

        for (String key : requiredTag.getAllKeys()) {
            if (!itemTag.contains(key)) { return !shouldInclude; }

            Tag itemValue = itemTag.get(key);
            Tag requiredValue = requiredTag.get(key);

            if (key.equalsIgnoreCase("Enchantments")) {
                Map<Enchantment, Integer> itemEnchants = EnchantmentHelper.deserializeEnchantments((ListTag) itemValue);
                Map<Enchantment, Integer> requiredEnchants = EnchantmentHelper.deserializeEnchantments((ListTag) requiredValue);

                for (Map. Entry<Enchantment, Integer> requiredEnchant : requiredEnchants.entrySet()) {
                    if (!itemEnchants.containsKey(requiredEnchant.getKey())) { return !shouldInclude; }

                    int itemEnchantValue = itemEnchants.get(requiredEnchant.getKey());
                    int requiredEnchantValue = requiredEnchants.get(requiredEnchant.getKey());
                    if (itemEnchantValue != requiredEnchantValue) { return !shouldInclude; }
                }
            } else if (itemValue instanceof CompoundTag && requiredValue instanceof CompoundTag) {
                boolean checked = checkNBTs((CompoundTag) itemValue, (CompoundTag) requiredValue, shouldInclude);
                if (!checked) { return !shouldInclude; }
            }
            else {
                if (!itemValue.equals(requiredValue)) { return !shouldInclude; }
            }

        }

        return true;
    }

    public static boolean containsRequiredTags(Object itemTag, Object compareTag, boolean shouldInclude) {
        // Si il n'y a rien à comparer, on est bon
        if (compareTag == null || (compareTag instanceof ListTag && ((ListTag) compareTag).isEmpty()) ||
                (compareTag instanceof CompoundTag && ((CompoundTag) compareTag).isEmpty())) return true;

        // Si les deux objets ne sont pas du même type, c'est mort
        if (!compareTag.getClass().equals(itemTag.getClass())) return false;

        if (compareTag instanceof CompoundTag) {

            // Pour chaque field de l'objet, on vérifie récursivement que l'item a le meme
            Set<String> itemKeys = ((CompoundTag) itemTag).getAllKeys();
            Set<String> compareKeys = ((CompoundTag) compareTag).getAllKeys();

            for (String compareKey : compareKeys) {
                if (itemKeys.contains(compareKey)) return false;

                if (!containsRequiredTags(
                        ((CompoundTag) itemTag).get(compareKey),
                        ((CompoundTag) compareTag).get(compareKey) ,
                        shouldInclude))
                    return false;
            }

            return true;
        }
        else if (compareTag instanceof ListTag) {

            // Pour chaque element de la liste, on vérifie récursivement qu'il est dans les tags de l'item
            for (var compareVal : ((ListTag)compareTag).stream().toList()) {
                for (var itemVal : ((ListTag)itemTag).stream().toList()) {
                    if (containsRequiredTags(itemVal, compareVal, shouldInclude)) {
                        break;
                    }
                    else {
                        return false;
                    }
                }
            }
            return true;
        }
        else {

            // Si on a atteint un primitif, on se contente de vérifier l'équivalence
            return compareTag.equals(itemTag);
        }
    }

    public static boolean containsRequiredTagsWithRange(Object itemTag, Object compareTag) {
        // Si il n'y a rien à comparer, on est bon
        if (compareTag == null || (compareTag instanceof ListTag && ((ListTag) compareTag).isEmpty()) ||
                (compareTag instanceof CompoundTag && ((CompoundTag) compareTag).isEmpty())) return true;

        if (compareTag instanceof CompoundTag) {
            Set<String> compareKeys = ((CompoundTag) compareTag).getAllKeys();

            // Si on a un format spécifique, on a un comportement spécifique
            if (compareKeys.size() == 1 && compareKeys.contains("RangeTag")) {

                // si on a un 'RangeTag' dans le compare, on veut un nombre (entier?)
                if (!(itemTag instanceof Integer)) return false;

                int[] compareItem = ((CompoundTag) compareTag).getIntArray("RangeTag");

                assert compareItem.length == 2 : "RangeTag too short";
                return compareItem[0] <= (int) itemTag && (int) itemTag <= compareItem[1];
            }

            // Sinon pour chaque field de l'objet, on vérifie récursivement que l'item a le meme
            else {
                if (!(itemTag instanceof CompoundTag)) return false;

                Set<String> itemKeys = ((CompoundTag) itemTag).getAllKeys();

                for (String compareKey : compareKeys) {
                    if (itemKeys.contains(compareKey)) return false;
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
                for (var itemVal : ((ListTag)itemTag).stream().toList()) {
                    if (containsRequiredTagsWithRange(itemVal, compareVal)) {
                        break;
                    }
                    else {
                        return false;
                    }
                }
            }

            return true;
        }
        else {
            // Si on a atteint un primitif, on se contente de vérifier l'équivalence
            return compareTag.getClass().equals(itemTag.getClass()) && compareTag.equals(itemTag);
        }
    }

    public static boolean containsRequiredTagsWithRangeBlacklist(Object itemTag, Object compareTag) {
        if (compareTag == null || (compareTag instanceof ListTag && ((ListTag) compareTag).isEmpty()) ||
                (compareTag instanceof CompoundTag && ((CompoundTag) compareTag).isEmpty())) return true;

        if (compareTag instanceof CompoundTag) {
            Set<String> compareKeys = ((CompoundTag) compareTag).getAllKeys();

            if (compareKeys.size() == 1 && compareKeys.contains("RangeTag")) {
                if (!(itemTag instanceof Integer)) return true;

                int[] compareItem = ((CompoundTag) compareTag).getIntArray("RangeTag");
                assert compareItem.length == 2 : "RangeTag too short";
                return ! (compareItem[0] <= (int) itemTag && (int) itemTag <= compareItem[1]);
            } else {
                if (!(itemTag instanceof CompoundTag)) return true;

                Set<String> itemKeys = ((CompoundTag) itemTag).getAllKeys();

                for (String compareKey : compareKeys) {
                    if (itemKeys.contains(compareKey) && containsRequiredTagsWithRangeBlacklist(
                            ((CompoundTag) itemTag).get(compareKey),
                            ((CompoundTag) compareTag).get(compareKey)))
                        return false;
                }
                return true;
            }
        } else if (compareTag instanceof ListTag) {
            for (var compareVal : ((ListTag) compareTag).stream().toList()) {
                for (var itemVal : ((ListTag) itemTag).stream().toList()) {
                    if (containsRequiredTagsWithRangeBlacklist(itemVal, compareVal)) {
                        return false;
                    }
                }
            }
            return true;
        } else {
            return !(compareTag.getClass().equals(itemTag.getClass()) && compareTag.equals(itemTag));
        }
    }

}