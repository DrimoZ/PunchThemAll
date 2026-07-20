package com.drimoz.punchthemall.core.checker;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ItemChecker {

    public static boolean doesItemExist(String itemName) {
        return BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
    }

    public static Item getExistingItem(String itemName) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemName));
    }

    public static Item getFirstItemFromTag(String itemTag) {
        return getItemsForTag(itemTag).stream().findFirst().orElse(null);
    }

    public static Set<Item> getItemsForTag(String itemTag) {
        TagKey<Item> tagKey = TagKey.create(Registries.ITEM, ResourceLocation.parse(itemTag));
        return BuiltInRegistries.ITEM.getTag(tagKey)
                .map(named -> named.stream().map(Holder::value).collect(Collectors.toCollection(HashSet::new)))
                .orElseGet(HashSet::new);
    }

    public static boolean isItemTagExisting(String itemTag) {
        TagKey<Item> tagKey = TagKey.create(Registries.ITEM, ResourceLocation.parse(itemTag));
        return BuiltInRegistries.ITEM.getTag(tagKey).isPresent();
    }
}
