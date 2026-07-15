package com.drimoz.punchthemall.core.checker;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class BlockChecker {

    public static boolean doesBlockExist(String blockName) {
        ResourceLocation blockResourceLocation = ResourceLocation.parse(blockName);
        return BuiltInRegistries.BLOCK.containsKey(blockResourceLocation);
    }

    public static Block getExistingBlock(String blockName) {
        ResourceLocation blockResourceLocation = ResourceLocation.parse(blockName);
        return BuiltInRegistries.BLOCK.getValue(blockResourceLocation);
    }

    public static Block getFirstBlockForTag(String blockTag) {
        return getBlocksForTag(blockTag).stream().findFirst().orElse(null);
    }

    public static Set<Block> getBlocksForTag(String blockTag) {
        ResourceLocation tagId = ResourceLocation.parse(blockTag);
        TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, tagId);

        return BuiltInRegistries.BLOCK.getTag(tagKey)
                .map(tag -> tag.stream().map(Holder::value).collect(Collectors.toCollection(HashSet::new)))
                .orElseGet(HashSet::new);
    }

    public static boolean isBlockTagExisting(String blockTag) {
        ResourceLocation tagId = ResourceLocation.parse(blockTag);
        TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, tagId);

        return BuiltInRegistries.BLOCK.getTag(tagKey).isPresent();
    }
}
