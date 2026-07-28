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

/** Registry lookups for fluids. A malformed id reports "does not exist"; see {@link ItemChecker}. */
public class FluidChecker {

    public static boolean doesFluidExist(String fluidName) {
        ResourceLocation id = ItemChecker.tryParse(fluidName);
        return id != null && BuiltInRegistries.FLUID.containsKey(id);
    }

    /**
     * @return the fluid, or {@code null} when the id is malformed or unregistered. Deliberately not
     *         the registry default, which would answer an unknown id with {@code EMPTY}.
     */
    public static Fluid getExistingFluid(String fluidName) {
        ResourceLocation id = ItemChecker.tryParse(fluidName);
        return id == null || !BuiltInRegistries.FLUID.containsKey(id) ? null : BuiltInRegistries.FLUID.get(id);
    }

    public static Fluid getFirstFluidForTag(String fluidTag) {
        return getFluidsForTag(fluidTag).stream().findFirst().orElse(null);
    }

    public static Set<Fluid> getFluidsForTag(String fluidTag) {
        TagKey<Fluid> tagKey = fluidTagKey(fluidTag);
        if (tagKey == null) return new HashSet<>();
        return BuiltInRegistries.FLUID.getTag(tagKey)
                .map(named -> named.stream().map(Holder::value).collect(Collectors.toCollection(HashSet::new)))
                .orElseGet(HashSet::new);
    }

    public static boolean isFluidTagExisting(String fluidTag) {
        TagKey<Fluid> tagKey = fluidTagKey(fluidTag);
        return tagKey != null && BuiltInRegistries.FLUID.getTag(tagKey).isPresent();
    }

    private static TagKey<Fluid> fluidTagKey(String fluidTag) {
        ResourceLocation id = ItemChecker.tryParse(fluidTag);
        return id == null ? null : TagKey.create(Registries.FLUID, id);
    }
}
