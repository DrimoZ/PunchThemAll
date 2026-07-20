package com.drimoz.punchthemall.core.util;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * PTA's stable, version-independent view of an item's data.
 *
 * <p>Since 1.20.5 items no longer expose a single {@code getTag()} — data lives in typed Data
 * Components. To keep the JSON authoring format identical across mod versions, PTA never exposes the
 * real item structure. Instead it maps an {@link ItemStack} to a fixed pseudo-NBT shape that matches
 * the historical 1.20.1 item NBT the format was designed around:</p>
 *
 * <pre>{ "Damage": int, "Enchantments": [ { "id": "...", "lvl": int } ], "custom": { …modded NBT… } }</pre>
 *
 * <p>All item matching (predicates and SNBT whitelist/blacklist) runs against this view, so a file's
 * {@code path}/{@code where} expressions work the same on Forge 1.20.1 and NeoForge 1.21.1. Any
 * version fragility is centralised here, not in pack JSON.</p>
 */
public final class ItemView {

    private ItemView() {}

    /** Read an item into the stable view. */
    public static CompoundTag of(ItemStack stack) {
        CompoundTag view = new CompoundTag();
        if (stack == null || stack.isEmpty()) return view;

        // Damage
        if (stack.isDamageableItem()) {
            view.putInt("Damage", stack.getDamageValue());
        } else if (stack.has(DataComponents.DAMAGE)) {
            view.putInt("Damage", stack.getOrDefault(DataComponents.DAMAGE, 0));
        }

        // Enchantments -> [{ id, lvl }]
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (!enchantments.isEmpty()) {
            ListTag list = new ListTag();
            for (var entry : enchantments.entrySet()) {
                Holder<Enchantment> holder = entry.getKey();
                CompoundTag e = new CompoundTag();
                holder.unwrapKey().ifPresent(key -> e.putString("id", key.location().toString()));
                e.putInt("lvl", entry.getIntValue());
                list.add(e);
            }
            view.put("Enchantments", list);
        }

        // Arbitrary / modded NBT lives in the custom_data component in 1.21.
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom != null && !custom.isEmpty()) {
            view.put("custom", custom.copyTag());
        }

        return view;
    }

    /**
     * Apply a stable-view compound (a drop's authored NBT) back onto an item. Damage is set directly;
     * everything else is stored in {@code custom_data}. Enchanted drops (which need level-time
     * registry access) are not applied here.
     */
    public static void applyTo(ItemStack stack, CompoundTag view) {
        if (stack == null || view == null || view.isEmpty()) return;

        CompoundTag custom = new CompoundTag();
        for (String key : view.getAllKeys()) {
            switch (key) {
                case "Damage" -> {
                    if (view.contains("Damage", Tag.TAG_ANY_NUMERIC)) {
                        stack.setDamageValue(view.getInt("Damage"));
                    }
                }
                case "Enchantments" -> {
                    // Enchanted drops require the dynamic enchantment registry; not supported here.
                }
                case "custom" -> {
                    if (view.get("custom") instanceof CompoundTag c) custom.merge(c);
                }
                default -> custom.put(key, view.get(key));
            }
        }

        if (!custom.isEmpty()) {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
        }
    }
}
