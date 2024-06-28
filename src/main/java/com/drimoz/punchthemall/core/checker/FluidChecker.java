package com.drimoz.punchthemall.core.checker;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITagManager;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class FluidChecker {

    public static boolean doesFluidExist(String fluidName) {
        ResourceLocation blockResourceLocation = new ResourceLocation(fluidName);
        return ForgeRegistries.FLUIDS.containsKey(blockResourceLocation);
    }

    public static Fluid getExistingFluid(String fluidName) {
        ResourceLocation blockResourceLocation = new ResourceLocation(fluidName);
        return ForgeRegistries.FLUIDS.getValue(blockResourceLocation);
    }

    public static Fluid getFirstFluidForTag(String fluidTag) {
        return getFluidsForTag(fluidTag).stream().findFirst().orElse(null);
    }

    public static Set<Fluid> getFluidsForTag(String fluidTag) {
        ResourceLocation tagId = new ResourceLocation(fluidTag);
        TagKey<Fluid> tagKey = TagKey.create(ForgeRegistries.FLUIDS.getRegistryKey(), tagId);
        ITagManager<Fluid> fluidTagManager = ForgeRegistries.FLUIDS.tags();

        if (fluidTagManager == null) {
            return new HashSet<>();
        }

        return fluidTagManager.getTag(tagKey).stream().collect(Collectors.toCollection(HashSet::new));
    }
}
