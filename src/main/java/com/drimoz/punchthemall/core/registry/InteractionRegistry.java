package com.drimoz.punchthemall.core.registry;

import com.drimoz.punchthemall.core.model.EInteractionType;
import com.drimoz.punchthemall.core.model.InteractedBlock;
import com.drimoz.punchthemall.core.model.Interaction;
import com.drimoz.punchthemall.core.util.PTALoggers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public List<Interaction> getInteractionsByInteractedBlockAndType(@Nullable Object state, EInteractionType type, boolean isShiftKeyDown) {
        EInteractionType eventType = EInteractionType.getTypeFromEvent(type, isShiftKeyDown);
        List<Interaction> result = new ArrayList<>();

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
        }
        return result;
    }

    // Inner work
}
