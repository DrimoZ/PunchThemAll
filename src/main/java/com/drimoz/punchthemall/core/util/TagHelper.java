package com.drimoz.punchthemall.core.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.Map;

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


}
