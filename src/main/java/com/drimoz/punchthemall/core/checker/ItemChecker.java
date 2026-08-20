package com.drimoz.punchthemall.core.checker;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITagManager;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Registry lookups for items.
 *
 * <p>Every entry point takes an authored string, so a malformed id is ordinary input rather than a
 * bug. {@link ResourceLocation#tryParse} is used throughout and an unparseable id reports "does not
 * exist"; the ResourceLocation constructor would throw from inside the reload listener and take the
 * whole datapack load down over one typo.</p>
 */
public class ItemChecker {

    public static boolean doesItemExist(String itemName) {
        ResourceLocation id = tryParse(itemName);
        return id != null && ForgeRegistries.ITEMS.containsKey(id);
    }

    /**
     * @return the item, or {@code null} when the id is malformed or unregistered. Deliberately not
     *         the registry default, which answers an unknown id with {@code AIR} — that reads as a
     *         real item and hides the mistake.
     */
    public static Item getExistingItem(String itemName) {
        ResourceLocation id = tryParse(itemName);
        return id == null || !ForgeRegistries.ITEMS.containsKey(id) ? null : ForgeRegistries.ITEMS.getValue(id);
    }

    public static Item getFirstItemFromTag(String itemTag) {
        return getItemsForTag(itemTag).stream().findFirst().orElse(null);
    }

    public static Set<Item> getItemsForTag(String itemTag) {
        ResourceLocation tagId = tryParse(itemTag);
        if (tagId == null) return new HashSet<>();

        TagKey<Item> tagKey = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), tagId);
        ITagManager<Item> itemTagManager = ForgeRegistries.ITEMS.tags();

        if (itemTagManager == null) {
            return new HashSet<>();
        }

        return itemTagManager.getTag(tagKey).stream().collect(Collectors.toCollection(HashSet::new));
    }

    /** Shared by the block and fluid checkers, so all three treat a bad id the same way. */
    static ResourceLocation tryParse(String id) {
        return id == null ? null : ResourceLocation.tryParse(id);
    }
}
