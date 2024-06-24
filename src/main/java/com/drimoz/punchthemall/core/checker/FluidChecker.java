package com.drimoz.punchthemall.core.checker;

import com.drimoz.punchthemall.core.util.PTALoggers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITagManager;

import java.util.Optional;

public class FluidChecker {

    public static boolean doesFluidExist(String fluidName) {
        ResourceLocation blockResourceLocation = new ResourceLocation(fluidName);
        return ForgeRegistries.FLUIDS.containsKey(blockResourceLocation);
    }

    public static Fluid getExistingFluid(String fluidName) {
        ResourceLocation blockResourceLocation = new ResourceLocation(fluidName);
        return ForgeRegistries.FLUIDS.getValue(blockResourceLocation);
    }

    public static Fluid getFirstFluidFromTag(String fluidTag) {
        ResourceLocation tagId = new ResourceLocation(fluidTag);
        TagKey<Fluid> tagKey = TagKey.create(ForgeRegistries.FLUIDS.getRegistryKey(), tagId);

        ITagManager<Fluid> blockTagManager = ForgeRegistries.FLUIDS.tags();

        if (blockTagManager == null) {
            return null;
        }

        Optional<Fluid> firstFluid = blockTagManager.getTag(tagKey)
                .stream()
                .findFirst();
        return firstFluid.orElse(null);
    }
}
