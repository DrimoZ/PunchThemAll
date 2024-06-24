package com.drimoz.punchthemall.core.checker;

import com.drimoz.punchthemall.core.util.PTALoggers;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITagManager;

import java.util.Optional;

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
        ResourceLocation tagId = new ResourceLocation(itemTag);
        TagKey<Item> tagKey = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), tagId);

        ITagManager<Item> itemTagManager = ForgeRegistries.ITEMS.tags();

        if (itemTagManager == null) {
            return null;
        }

        PTALoggers.error("GOOD - CONTINUE - " + tagKey);
        PTALoggers.error("GOOD - CONTINUE - " + itemTagManager.getTag(tagKey));

        Optional<Item> firstItem = itemTagManager.getTag(tagKey)
                .stream()
                .findFirst();
        return firstItem.orElse(null);
    }
}
