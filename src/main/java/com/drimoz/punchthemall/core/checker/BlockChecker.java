package com.drimoz.punchthemall.core.checker;

import com.drimoz.punchthemall.core.util.PTALoggers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITagManager;

import java.util.Optional;

public class BlockChecker {

    public static boolean doesBlockExist(String blockName) {
        ResourceLocation blockResourceLocation = new ResourceLocation(blockName);
        return ForgeRegistries.BLOCKS.containsKey(blockResourceLocation);
    }

    public static Block getExistingBlock(String blockName) {
        ResourceLocation blockResourceLocation = new ResourceLocation(blockName);
        return ForgeRegistries.BLOCKS.getValue(blockResourceLocation);
    }

    public static Block getFirstBlockFromTag(String blockTag) {
        ResourceLocation tagId = new ResourceLocation(blockTag);
        TagKey<Block> tagKey = TagKey.create(ForgeRegistries.BLOCKS.getRegistryKey(), tagId);

        ITagManager<Block> blockTagManager = ForgeRegistries.BLOCKS.tags();

        if (blockTagManager == null) {
            return null;
        }

        PTALoggers.error("GOOD - CONTINUE - " + tagKey);
        PTALoggers.error("GOOD - CONTINUE - " + blockTagManager.getTag(tagKey));

        Optional<Block> firstBlock = blockTagManager.getTag(tagKey)
                .stream()
                .findFirst();
        return firstBlock.orElse(null);
    }
}
