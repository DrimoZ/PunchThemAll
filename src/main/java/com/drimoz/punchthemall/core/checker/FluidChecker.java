package com.drimoz.punchthemall.core.checker;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITagManager;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class FluidChecker {

    public static boolean doesFluidExist(String fluidName) {
        ResourceLocation id = ItemChecker.tryParse(fluidName);
        return id != null && ForgeRegistries.FLUIDS.containsKey(id);
    }

    /**
     * @return the fluid, or {@code null} when the id is malformed or unregistered. Deliberately not
     *         the registry default, which would answer an unknown id with empty fluid.
     */
    public static Fluid getExistingFluid(String fluidName) {
        ResourceLocation id = ItemChecker.tryParse(fluidName);
        return id == null || !ForgeRegistries.FLUIDS.containsKey(id) ? null : ForgeRegistries.FLUIDS.getValue(id);
    }

    public static Fluid getFirstFluidForTag(String fluidTag) {
        return getFluidsForTag(fluidTag).stream().findFirst().orElse(null);
    }

    public static Set<Fluid> getFluidsForTag(String fluidTag) {
        ResourceLocation tagId = ItemChecker.tryParse(fluidTag);
        if (tagId == null) return new HashSet<>();

        TagKey<Fluid> tagKey = TagKey.create(ForgeRegistries.FLUIDS.getRegistryKey(), tagId);
        ITagManager<Fluid> fluidTagManager = ForgeRegistries.FLUIDS.tags();

        if (fluidTagManager == null) {
            return new HashSet<>();
        }

        return fluidTagManager.getTag(tagKey).stream().collect(Collectors.toCollection(HashSet::new));
    }

    public static boolean isFluidTagExisting(String fluidTag) {
        ResourceLocation tagId = ItemChecker.tryParse(fluidTag);
        if (tagId == null) return false;

        TagKey<Fluid> tagKey = TagKey.create(ForgeRegistries.FLUIDS.getRegistryKey(), tagId);
        ITagManager<Fluid> fluidTagManager = ForgeRegistries.FLUIDS.tags();

        return fluidTagManager != null && fluidTagManager.getTagNames().anyMatch(fluidTagKey -> fluidTagKey.location().equals(tagKey.location()));
    }
}
