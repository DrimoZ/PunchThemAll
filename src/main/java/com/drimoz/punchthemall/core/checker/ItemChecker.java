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

/**
 * Registry lookups for items.
 *
 * <p>Every entry point takes an authored string, so a malformed id is ordinary input rather than a
 * bug. {@link ResourceLocation#tryParse} is used throughout and an unparseable id reports "does not
 * exist"; {@code ResourceLocation.parse} would throw from inside the reload listener and take the
 * whole datapack load down over one typo.</p>
 */
public class ItemChecker {

    public static boolean doesItemExist(String itemName) {
        ResourceLocation id = tryParse(itemName);
        return id != null && BuiltInRegistries.ITEM.containsKey(id);
    }

    /**
     * @return the item, or {@code null} when the id is malformed or unregistered. Deliberately not
     *         the registry default: {@code Registry.get} answers an unknown id with {@code AIR},
     *         which reads as a real item and hides the mistake.
     */
    public static Item getExistingItem(String itemName) {
        ResourceLocation id = tryParse(itemName);
        return id == null || !BuiltInRegistries.ITEM.containsKey(id) ? null : BuiltInRegistries.ITEM.get(id);
    }

    public static Item getFirstItemFromTag(String itemTag) {
        return getItemsForTag(itemTag).stream().findFirst().orElse(null);
    }

    public static Set<Item> getItemsForTag(String itemTag) {
        TagKey<Item> tagKey = itemTagKey(itemTag);
        if (tagKey == null) return new HashSet<>();
        return BuiltInRegistries.ITEM.getTag(tagKey)
                .map(named -> named.stream().map(Holder::value).collect(Collectors.toCollection(HashSet::new)))
                .orElseGet(HashSet::new);
    }

    public static boolean isItemTagExisting(String itemTag) {
        TagKey<Item> tagKey = itemTagKey(itemTag);
        return tagKey != null && BuiltInRegistries.ITEM.getTag(tagKey).isPresent();
    }

    private static TagKey<Item> itemTagKey(String itemTag) {
        ResourceLocation id = tryParse(itemTag);
        return id == null ? null : TagKey.create(Registries.ITEM, id);
    }

    /** {@code ResourceLocation.tryParse} rejects malformed ids but still throws on null. */
    static ResourceLocation tryParse(String id) {
        return id == null ? null : ResourceLocation.tryParse(id);
    }
}
