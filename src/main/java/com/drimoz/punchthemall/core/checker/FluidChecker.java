package com.drimoz.punchthemall.core.checker;

import net.minecraft.core.Holder;
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
        return BuiltInRegistries.FLUID.containsKey(ResourceLocation.parse(fluidName));
    }

    public static Fluid getExistingFluid(String fluidName) {
        return BuiltInRegistries.FLUID.get(ResourceLocation.parse(fluidName));
    }

    public static Fluid getFirstFluidForTag(String fluidTag) {
        return getFluidsForTag(fluidTag).stream().findFirst().orElse(null);
    }

    public static Set<Fluid> getFluidsForTag(String fluidTag) {
        TagKey<Fluid> tagKey = TagKey.create(Registries.FLUID, ResourceLocation.parse(fluidTag));
        return BuiltInRegistries.FLUID.getTag(tagKey)
                .map(named -> named.stream().map(Holder::value).collect(Collectors.toCollection(HashSet::new)))
                .orElseGet(HashSet::new);
    }

    public static boolean isFluidTagExisting(String fluidTag) {
        TagKey<Fluid> tagKey = TagKey.create(Registries.FLUID, ResourceLocation.parse(fluidTag));
        return BuiltInRegistries.FLUID.getTag(tagKey).isPresent();
    }
}
