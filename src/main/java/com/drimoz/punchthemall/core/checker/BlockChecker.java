package com.drimoz.punchthemall.core.checker;

import com.drimoz.punchthemall.core.util.PTALoggers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITagManager;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class BlockChecker {

    public static boolean doesBlockExist(String blockName) {
        ResourceLocation blockResourceLocation = new ResourceLocation(blockName);
        return ForgeRegistries.BLOCKS.containsKey(blockResourceLocation);
    }

    public static Block getExistingBlock(String blockName) {
        ResourceLocation blockResourceLocation = new ResourceLocation(blockName);
        return ForgeRegistries.BLOCKS.getValue(blockResourceLocation);
    }

    public static Block getFirstBlockForTag(String blockTag) {
        return getBlocksForTag(blockTag).stream().findFirst().orElse(null);
    }

    public static Set<Block> getBlocksForTag(String blockTag) {
        ResourceLocation tagId = new ResourceLocation(blockTag);
        TagKey<Block> tagKey = TagKey.create(ForgeRegistries.BLOCKS.getRegistryKey(), tagId);
        ITagManager<Block> blockTagManager = ForgeRegistries.BLOCKS.tags();

        if (blockTagManager == null) {
            return new HashSet<>();
        }

        return blockTagManager.getTag(tagKey).stream().collect(Collectors.toCollection(HashSet::new));
    }

    public static boolean isBlockTagExisting(String blockTag) {
        ResourceLocation tagId = new ResourceLocation(blockTag);
        TagKey<Block> tagKey = TagKey.create(ForgeRegistries.BLOCKS.getRegistryKey(), tagId);
        ITagManager<Block> blockTagManager = ForgeRegistries.BLOCKS.tags();

        return blockTagManager != null && blockTagManager.getTagNames().anyMatch(blockTagKey -> blockTagKey.location().equals(tagKey.location()));
    }
}
