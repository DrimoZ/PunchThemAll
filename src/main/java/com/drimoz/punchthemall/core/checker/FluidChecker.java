package com.drimoz.punchthemall.core.checker;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class FluidChecker {

    public static boolean doesFluidExist(String fluidName) {
        ResourceLocation blockResourceLocation = ResourceLocation.parse(fluidName);
        return BuiltInRegistries.FLUID.containsKey(blockResourceLocation);
    }

    public static Fluid getExistingFluid(String fluidName) {
        ResourceLocation blockResourceLocation = ResourceLocation.parse(fluidName);
        return BuiltInRegistries.FLUID.get(blockResourceLocation);
    }

    public static Fluid getFirstFluidForTag(String fluidTag) {
        return getFluidsForTag(fluidTag).stream().findFirst().orElse(null);
    }

    public static Set<Fluid> getFluidsForTag(String fluidTag) {
        ResourceLocation tagId = ResourceLocation.parse(fluidTag);
        TagKey<Fluid> tagKey = TagKey.create(Registries.FLUID, tagId);
        return BuiltInRegistries.FLUID.stream()
                .filter(fluid -> fluid.builtInRegistryHolder().is(tagKey))
                .collect(Collectors.toCollection(HashSet::new));
    }

    public static boolean isFluidTagExisting(String fluidTag) {
        ResourceLocation tagId = ResourceLocation.parse(fluidTag);
        TagKey<Fluid> tagKey = TagKey.create(Registries.FLUID, tagId);
        return BuiltInRegistries.FLUID.getTagNames().anyMatch(fluidTagKey -> fluidTagKey.location().equals(tagKey.location()));
    }
}
