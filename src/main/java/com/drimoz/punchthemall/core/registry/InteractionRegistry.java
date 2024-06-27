package com.drimoz.punchthemall.core.registry;

import com.drimoz.punchthemall.core.model.*;
import com.drimoz.punchthemall.core.util.PTALoggers;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

import javax.annotation.Nullable;
import java.util.*;

public class InteractionRegistry {

    // Private properties

    private static final InteractionRegistry INSTANCE = new InteractionRegistry();
    private final Map<ResourceLocation, Interaction> interactions = new HashMap<>();

    // Life cycle

    private InteractionRegistry() {}

    // Interface

    public static InteractionRegistry getInstance() {
        return INSTANCE;
    }

    public Map<ResourceLocation, Interaction> getInteractions() {
        return interactions;
    }

    public void addInteraction(Interaction interaction) {
        interactions.put(interaction.getId(), interaction);
    }

    public Interaction getInteractionById(ResourceLocation id) {
        return interactions.get(id);
    }

    public List<Interaction> getCorrectInteractions(
            EInteractionType interactionType, boolean clickOnBlock,
            Player player, BlockPos pos, Level level
    ) {
        List<Interaction> result = new ArrayList<>();
        EInteractionType eventType = EInteractionType.getTypeFromEvent(interactionType, player.isShiftKeyDown());

        for (Interaction interaction : interactions.values()) {
            // Filter : InteractionType
            if (!interaction.getInteractionType().equals(eventType)) continue;

            // Filter : Biome
            // Filter : Dimension
            // TODO : Biome and Dimension conditions

            // Filter : OnAir or OnBlock
            if (clickOnBlock && interaction.isAir()) continue;
            if (!clickOnBlock && !interaction.isAir()) continue;

            // Filter : Corresponding Block / BlockState
            boolean matchBlock = true;
            if (clickOnBlock) {
                // Block
                if (interaction.getInteractedBlock().getBlockBase().isBlock()) {
                    BlockState worldBlockState = level.getBlockState(pos);
                    Block worldBlock = worldBlockState.getBlock();

                    // Block
                    if (!worldBlock.equals(interaction.getInteractedBlock().getBlockBase().getBlock())) continue;

                    // BlockState
                    for(var entry : interaction.getInteractedBlock().getBlockBase().getStateEntries()) {
                        if (!worldBlockState.getValues().containsKey(entry.getProperty())) matchBlock = false;
                        if (!worldBlockState.getValue(entry.getProperty()).equals(entry.getValue())) matchBlock = false;
                    }
                }
                // Fluid
                else {
                    FluidState worldFluidState = level.getFluidState(pos);
                    Fluid worldFluid = worldFluidState.getType();

                    // Fluid
                    if (!worldFluid.equals(interaction.getInteractedBlock().getBlockBase().getFluid())) continue;

                    // FluidState
                    for(var entry : interaction.getInteractedBlock().getBlockBase().getStateEntries()) {
                        if (!worldFluidState.getValues().containsKey(entry.getProperty())) matchBlock = false;
                        if (!worldFluidState.getValue(entry.getProperty()).equals(entry.getValue())) matchBlock = false;
                    }
                }
            }
            if (!matchBlock) continue;

            // Filter : NBT
            BlockEntity worldBlockEntity = level.getBlockEntity(pos);

            if (worldBlockEntity != null && !interaction.getInteractedBlock().getBlockBase().getNbt().isEmpty()) {
                CompoundTag worldBlockEntityTag = worldBlockEntity.serializeNBT();

                for (String key : interaction.getInteractedBlock().getBlockBase().getNbt().getAllKeys()) {
                    if (!worldBlockEntityTag.contains(key)) matchBlock = false;
                    else if (!Objects.equals(interaction.getInteractedBlock().getBlockBase().getNbt().get(key), worldBlockEntityTag.get(key))) matchBlock = false;
                }
            }

            if (!matchBlock) continue;

            // Filter : Hand
            ItemStack playerMainHandItem = player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND);
            ItemStack playerOffHandItem = player.getItemInHand(net.minecraft.world.InteractionHand.OFF_HAND);
            boolean matchHand = true;

            if (interaction.getInteractionHand() == null) {
                if (!playerMainHandItem.isEmpty() || !playerOffHandItem.isEmpty()) matchHand = false;
            }
            else if (interaction.getInteractionHand().getItemStack().isEmpty()) {
                switch (interaction.getInteractionHand().getInteractionHandType()) {
                    case ANY_HAND -> {
                        if (!playerMainHandItem.isEmpty() || !playerOffHandItem.isEmpty()) matchHand = false;
                    }
                    case MAIN_HAND -> {
                        if (!playerMainHandItem.isEmpty()) matchHand = false;
                    }
                    case OFF_HAND -> {
                        if (!playerOffHandItem.isEmpty()) matchHand = false;
                    }
                }
            }
            else {
                switch (interaction.getInteractionHand().getInteractionHandType()) {
                    case ANY_HAND -> {
                        if (playerMainHandItem.is(interaction.getInteractionHand().getItemStack().getItem())) {
                            if (interaction.getInteractionHand().getItemStack().hasTag() && !interaction.getInteractionHand().getItemStack().getTag().isEmpty()) {
                                if (!playerMainHandItem.hasTag() || playerMainHandItem.getTag().isEmpty()) matchHand = false;
                                if (!matchHand) continue;

                                for (String key : interaction.getInteractionHand().getItemStack().getTag().getAllKeys()) {
                                    if (!playerMainHandItem.getTag().contains(key)) matchHand = false;
                                    else if (!Objects.equals(interaction.getInteractionHand().getItemStack().getTag().get(key), playerMainHandItem.getTag().get(key))) matchHand = false;
                                }
                            }
                        }
                        else if (playerOffHandItem.is(interaction.getInteractionHand().getItemStack().getItem())) {
                            if (interaction.getInteractionHand().getItemStack().hasTag() && !interaction.getInteractionHand().getItemStack().getTag().isEmpty()) {
                                if (!playerOffHandItem.hasTag() || playerOffHandItem.getTag().isEmpty()) matchHand = false;
                                if (!matchHand) continue;

                                for (String key : interaction.getInteractionHand().getItemStack().getTag().getAllKeys()) {
                                    if (!playerOffHandItem.getTag().contains(key)) matchHand = false;
                                    else if (!Objects.equals(interaction.getInteractionHand().getItemStack().getTag().get(key), playerOffHandItem.getTag().get(key))) matchHand = false;
                                }
                            }
                        }
                        else {
                            matchHand = false;
                        }
                    }
                    case MAIN_HAND -> {
                        if (!playerMainHandItem.is(interaction.getInteractionHand().getItemStack().getItem())) matchHand = false;
                        if (!matchHand) continue;

                        if (interaction.getInteractionHand().getItemStack().hasTag() && !interaction.getInteractionHand().getItemStack().getTag().isEmpty()) {
                            if (!playerMainHandItem.hasTag() || playerMainHandItem.getTag().isEmpty()) matchHand = false;
                            if (!matchHand) continue;

                            for (String key : interaction.getInteractionHand().getItemStack().getTag().getAllKeys()) {
                                if (!playerMainHandItem.getTag().contains(key)) matchHand = false;
                                else if (!Objects.equals(interaction.getInteractionHand().getItemStack().getTag().get(key), playerMainHandItem.getTag().get(key))) matchHand = false;
                            }
                        }
                    }
                    case OFF_HAND -> {
                        if (!playerOffHandItem.is(interaction.getInteractionHand().getItemStack().getItem())) matchHand = false;
                        if (!matchHand) continue;

                        if (interaction.getInteractionHand().getItemStack().hasTag() && !interaction.getInteractionHand().getItemStack().getTag().isEmpty()) {
                            if (!playerOffHandItem.hasTag() || playerOffHandItem.getTag().isEmpty()) matchHand = false;
                            if (!matchHand) continue;

                            for (String key : interaction.getInteractionHand().getItemStack().getTag().getAllKeys()) {
                                if (!playerOffHandItem.getTag().contains(key)) matchHand = false;
                                else if (!Objects.equals(interaction.getInteractionHand().getItemStack().getTag().get(key), playerOffHandItem.getTag().get(key))) matchHand = false;
                            }
                        }
                    }
                }
            }
            if (!matchHand) continue;

            result.add(interaction);
        }

        return result;
    }

    public int getJEIRowCount() {
        int maxPool = 0;

        for (Interaction interaction : interactions.values()) {
            if (maxPool < interaction.getDropPool().size())
                maxPool = interaction.getDropPool().size();
        }

        return (int) Math.ceil(maxPool / 9.0);
    }

    public List<Interaction> getInteractionsByInteractedBlockAndType(@Nullable Object state, EInteractionType type, boolean isShiftKeyDown) {
        EInteractionType eventType = EInteractionType.getTypeFromEvent(type, isShiftKeyDown);
        List<Interaction> result = new ArrayList<>();
    /*
        for (Interaction interaction : interactions.values()) {
            InteractedBlock interactedBlock = interaction.getInteractedBlock();

            // Check if the interaction matches a block
            if (state instanceof BlockState && interactedBlock.getBlockAsBlockState() != null) {
                BlockState blockState = (BlockState) state;
                Block interactionBlock = interactedBlock.getBlockAsBlock();
                BlockState interactionBlockState = interactedBlock.getBlockAsBlockState();

                PTALoggers.error("World BlockState : " + blockState);
                PTALoggers.error("Interaction Block : " + interactionBlock);
                PTALoggers.error("Interaction BlockState : " + interactionBlockState);

                // If block states are provided, check if they match
                if (interactionBlockState.equals(blockState) && interaction.getType() == eventType) {
                    result.add(interaction);
                }
                // Otherwise, just check if the blocks match
                //else if (block.equals(interactionBlock) && interaction.getType() == eventType) {
                //    result.add(interaction);
                //}
            }

            // Check if the interaction matches a fluid
            else if (state instanceof FluidState && interactedBlock.getBlockAsFluidState() != null) {
                FluidState fluidState = (FluidState) state;
                Fluid interactionFluid = interactedBlock.getBlockAsFluid();
                FluidState interactionFluidState = interactedBlock.getBlockAsFluidState();

                // If fluid states are provided, check if they match
                if (interactionFluidState.equals(fluidState) && interaction.getType() == eventType) {
                    result.add(interaction);
                }
                // Otherwise, just check if the fluids match
                //else if (fluid.equals(interactionFluid) && interaction.getType() == eventType) {
                //    result.add(interaction);
                //}
            }

            // Check if the interaction matches air (i.e., no block or fluid)
            else if (state == null && interaction.isAir() && interaction.getType() == eventType) {
                result.add(interaction);
            }
        }*/
        return result;
    }
}
