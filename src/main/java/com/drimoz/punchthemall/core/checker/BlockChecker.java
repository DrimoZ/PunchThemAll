package com.drimoz.punchthemall.core.checker;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Registry lookups for blocks. A malformed id reports "does not exist"; see {@link ItemChecker}. */
public class BlockChecker {

    public static boolean doesBlockExist(String blockName) {
        Identifier id = ItemChecker.tryParse(blockName);
        return id != null && BuiltInRegistries.BLOCK.containsKey(id);
    }

    /**
     * @return the block, or {@code null} when the id is malformed or unregistered. Deliberately not
     *         the registry default, which would answer an unknown id with {@code AIR}.
     */
    public static Block getExistingBlock(String blockName) {
        Identifier id = ItemChecker.tryParse(blockName);
        return id == null || !BuiltInRegistries.BLOCK.containsKey(id) ? null : BuiltInRegistries.BLOCK.getValue(id);
    }

    public static Block getFirstBlockForTag(String blockTag) {
        return getBlocksForTag(blockTag).stream().findFirst().orElse(null);
    }

    public static Set<Block> getBlocksForTag(String blockTag) {
        TagKey<Block> tagKey = blockTagKey(blockTag);
        if (tagKey == null) return new HashSet<>();
        return BuiltInRegistries.BLOCK.get(tagKey)
                .map(named -> named.stream().map(Holder::value).collect(Collectors.toCollection(HashSet::new)))
                .orElseGet(HashSet::new);
    }

    public static boolean isBlockTagExisting(String blockTag) {
        TagKey<Block> tagKey = blockTagKey(blockTag);
        return tagKey != null && BuiltInRegistries.BLOCK.get(tagKey).isPresent();
    }

    private static TagKey<Block> blockTagKey(String blockTag) {
        Identifier id = ItemChecker.tryParse(blockTag);
        return id == null ? null : TagKey.create(Registries.BLOCK, id);
    }
}
