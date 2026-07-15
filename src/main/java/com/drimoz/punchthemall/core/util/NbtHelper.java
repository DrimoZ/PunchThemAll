package com.drimoz.punchthemall.core.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Centralizes NBT/custom-data compatibility helpers for Minecraft 26.1.
 *
 * <p>ItemStack free-form NBT now lives in the {@link DataComponents#CUSTOM_DATA}
 * data component. Block-entity save/load operations also need the world's
 * registry access so components and registry-backed payloads can be decoded.</p>
 */
public final class NbtHelper {
    private NbtHelper() {
    }

    public static CompoundTag getCustomData(ItemStack stack) {
        if (stack.isEmpty()) {
            return new CompoundTag();
        }
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static void setCustomData(ItemStack stack, CompoundTag tag) {
        if (stack.isEmpty() || tag == null || tag.isEmpty()) {
            return;
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag.copy()));
    }

    public static CompoundTag saveBlockEntity(BlockEntity blockEntity, Level level) {
        return blockEntity.saveWithFullMetadata(level.registryAccess());
    }

    public static void loadBlockEntity(BlockEntity blockEntity, Level level, CompoundTag tag) {
        blockEntity.loadWithComponents(tag, level.registryAccess());
        blockEntity.setChanged();
    }
}
