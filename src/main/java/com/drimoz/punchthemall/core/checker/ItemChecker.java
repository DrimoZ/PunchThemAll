package com.drimoz.punchthemall.core.checker;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITagManager;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ItemChecker {

    public static boolean doesItemExist(String itemName) {
        ResourceLocation itemResourceLocation = new ResourceLocation(itemName);
        return ForgeRegistries.ITEMS.containsKey(itemResourceLocation);
    }

    public static Item getExistingItem(String itemName) {
        ResourceLocation itemResourceLocation = new ResourceLocation(itemName);
        return ForgeRegistries.ITEMS.getValue(itemResourceLocation);
    }

    public static Item getFirstItemFromTag(String itemTag) {
        return getItemsForTag(itemTag).stream().findFirst().orElse(null);
    }

    public static Set<Item> getItemsForTag(String itemTag) {
        ResourceLocation tagId = new ResourceLocation(itemTag);
        TagKey<Item> tagKey = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), tagId);
        ITagManager<Item> itemTagManager = ForgeRegistries.ITEMS.tags();

        if (itemTagManager == null) {
            return new HashSet<>();
        }

        return itemTagManager.getTag(tagKey).stream().collect(Collectors.toCollection(HashSet::new));
    }

}
