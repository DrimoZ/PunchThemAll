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
        ResourceLocation itemResourceLocation = ResourceLocation.parse(itemName);
        return BuiltInRegistries.ITEM.containsKey(itemResourceLocation);
    }

    public static Item getExistingItem(String itemName) {
        ResourceLocation itemResourceLocation = ResourceLocation.parse(itemName);
        return BuiltInRegistries.ITEM.getValue(itemResourceLocation);
    }

    public static Item getFirstItemFromTag(String itemTag) {
        return getItemsForTag(itemTag).stream().findFirst().orElse(null);
    }

    public static Set<Item> getItemsForTag(String itemTag) {
        ResourceLocation tagId = ResourceLocation.parse(itemTag);
        TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);

        return BuiltInRegistries.ITEM.getTag(tagKey)
                .map(tag -> tag.stream().map(Holder::value).collect(Collectors.toCollection(HashSet::new)))
                .orElseGet(HashSet::new);
    }

}
