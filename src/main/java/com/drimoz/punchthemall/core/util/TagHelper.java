package com.drimoz.punchthemall.core.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;

import java.util.Set;

public class TagHelper {
    private TagHelper() {}

    public static boolean containsRequiredTagsWithRange(Object itemTag, Object compareTag) {
        // Si il n'y a rien à comparer, on est bon
        if (compareTag == null || (compareTag instanceof ListTag && ((ListTag) compareTag).isEmpty()) ||
                (compareTag instanceof CompoundTag && ((CompoundTag) compareTag).isEmpty())) return true;

        if (compareTag instanceof CompoundTag) {
            Set<String> compareKeys = ((CompoundTag) compareTag).getAllKeys();

            // Si on a un format spécifique, on a un comportement spécifique.
            // On compare numériquement plutôt que par type de tag : le SNBT écrit librement `[0,500]`
            // (int) ou `[2s,7s]` (short), et la valeur lue sur l'item n'a aucune raison d'utiliser la
            // même largeur. Comparer les types exacts faisait silencieusement échouer des fichiers
            // valides — et lançait une ClassCastException quand les deux largeurs différaient.
            // Le chemin blacklist ci-dessous fait déjà cela ; les deux sont maintenant alignés.
            if (compareKeys.size() == 1 && compareKeys.contains("RangeTag")) {
                if (!(itemTag instanceof NumericTag itemNumeric)) return false;

                // `assert` ne sert à rien ici : les assertions sont désactivées par défaut en jeu,
                // donc un RangeTag mal formé partait en IndexOutOfBounds au lieu d'être rejeté.
                if (!(((CompoundTag) compareTag).get("RangeTag") instanceof ListTag listRangeTag)
                        || listRangeTag.size() != 2
                        || !(listRangeTag.get(0) instanceof NumericTag minTag)
                        || !(listRangeTag.get(1) instanceof NumericTag maxTag)) {
                    return false;
                }

                long value = itemNumeric.getAsLong();
                return minTag.getAsLong() <= value && value <= maxTag.getAsLong();
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
            // A list is required, so anything that is not a list cannot satisfy it. The authored NBT
            // and the value read from the world are independent, so a shape mismatch here is normal
            // input (another mod's item NBT, a block entity that changed layout) — never a cast.
            if (!(itemTag instanceof ListTag itemList)) return false;

            // Pour chaque element de la liste, on vérifie récursivement qu'il est dans les tags de l'item
            for (var compareVal : ((ListTag)compareTag).stream().toList()) {
                boolean test = false;
                for (var itemVal : itemList.stream().toList()) {
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
            return itemTag != null && compareTag.getClass().equals(itemTag.getClass()) && compareTag.equals(itemTag);
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
                // Un RangeTag en blacklist interdit les valeurs DANS [min, max] ; tout le reste passe.
                // Toute valeur numérique est acceptée, quelle que soit sa largeur — même règle que
                // la whitelist ci-dessus.
                if (!(itemTag instanceof NumericTag itemNumeric)) return true;
                long value = itemNumeric.getAsLong();

                // On vérifie que la valeur liée à RangeTag soit une list de deux bornes
                if (!(((CompoundTag) compareTag).get("RangeTag") instanceof ListTag listRangeTag)
                        || listRangeTag.size() != 2
                        || !(listRangeTag.get(0) instanceof NumericTag minTag)
                        || !(listRangeTag.get(1) instanceof NumericTag maxTag)) {
                    return true;
                }

                return value < minTag.getAsLong() || value > maxTag.getAsLong();
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
            // Nothing that is not a list can hold a forbidden element, so it passes. Mirrors the
            // whitelist guard above: a shape mismatch is input, not a cast failure.
            if (!(itemTag instanceof ListTag itemList)) return true;

            // Pour chaque element de la liste, on vérifie récursivement qu'il est dans les tags de l'item
            for (var compareVal : ((ListTag) compareTag).stream().toList()) {
                boolean test = true;
                for (var itemVal : itemList.stream().toList()) {
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
            return itemTag == null || !compareTag.getClass().equals(itemTag.getClass()) || !compareTag.equals(itemTag);
        }
    }

}