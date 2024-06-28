package com.drimoz.punchthemall.core.registry;

import com.drimoz.punchthemall.core.model.*;
import com.drimoz.punchthemall.core.util.PTALoggers;
import com.drimoz.punchthemall.core.util.TagHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

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

    public List<Interaction> getFilteredInteractions(
            EInteractionType interactionType, boolean clickOnBlock,
            Player player, BlockPos pos, Level level
    ) {
        List<Interaction> filteredInteractions = new ArrayList<>();

        EInteractionType eventType = EInteractionType.getTypeFromEvent(interactionType, player.isShiftKeyDown());

        for (Interaction interaction : interactions.values()) {
            if (!passesInteractionFilters(interaction, eventType, clickOnBlock, player, pos, level)) {
                continue;
            }
            filteredInteractions.add(interaction);
        }

        return filteredInteractions;
    }

    public int getJEIRowCount() {
        int maxPool = 0;

        for (Interaction interaction : interactions.values()) {
            if (maxPool < interaction.getDropPool().size())
                maxPool = interaction.getDropPool().size();
        }

        return (int) Math.ceil(maxPool / 9.0);
    }

    // Inner work ( Interaction Filter )

    private boolean passesInteractionFilters(
            Interaction interaction, EInteractionType eventType, boolean clickOnBlock,
            Player player, BlockPos pos, Level level
    ) {
        return passesInteractionTypeFilter(interaction, eventType) &&
                passesBiomeAndDimensionFilter(interaction, level, pos) &&
                passesAirOrBlockFilter(interaction, clickOnBlock) &&
                passesBlockStateFilter(interaction, clickOnBlock, pos, level) &&
                passesBlockEntityNBTFilter(interaction, clickOnBlock, pos, level) &&
                passesHandItemFilter(interaction, player);
    }

    private boolean passesInteractionTypeFilter(Interaction interaction, EInteractionType eventType) {
        return interaction.getInteractionType().equals(eventType);
    }

    private boolean passesBiomeAndDimensionFilter(Interaction interaction, Level level, BlockPos pos) {
        String playerDimensionId = level.dimension().location().toString();
        String playerBiomeId = level.getBiome(pos).unwrapKey().get().location().toString();

        boolean isBiomeWhitelist = interaction.isBiomeWhitelist();
        boolean matchesBiome = interaction.getBiomes().contains(playerBiomeId) || interaction.getBiomes().contains(playerDimensionId);

        return isBiomeWhitelist == matchesBiome;
    }

    private boolean passesAirOrBlockFilter(Interaction interaction, boolean clickOnBlock) {
        return interaction.interactWithAir() == !clickOnBlock;
    }

    private boolean passesBlockStateFilter(Interaction interaction, boolean clickOnBlock, BlockPos pos, Level level) {
        if (!clickOnBlock) {
            return true;
        }

        InteractedBlock interactedBlock = interaction.getInteractedBlock();
        InteractionBlock interactedBlockBase = interactedBlock.getBlockBase();

        if (interactedBlockBase.isBlock()) {
            BlockState worldBlockState = level.getBlockState(pos);
            Block worldBlock = worldBlockState.getBlock();

            if (!worldBlock.equals(interactedBlockBase.getBlock())) {
                return false;
            }

            return matchesBlockState(worldBlockState, interactedBlockBase.getStateEntries());
        } else {
            FluidState worldFluidState = level.getFluidState(pos);
            Fluid worldFluid = worldFluidState.getType();

            if (!worldFluid.equals(interactedBlockBase.getFluid())) {
                return false;
            }

            return matchesBlockState(worldFluidState.createLegacyBlock(), interactedBlockBase.getStateEntries());
        }
    }

    private boolean passesBlockEntityNBTFilter(Interaction interaction, boolean clickOnBlock, BlockPos pos, Level level) {
        if (!clickOnBlock || interaction.getInteractedBlock().getBlockBase().getNbt().isEmpty()) {
            return true;
        }

        BlockEntity worldBlockEntity = level.getBlockEntity(pos);

        if (worldBlockEntity == null) {
            return false;
        }

        CompoundTag worldBlockEntityTag = worldBlockEntity.serializeNBT();

        return TagHelper.checkNBTs(worldBlockEntityTag, interaction.getInteractedBlock().getBlockBase().getNbt(), true);
    }

    private boolean passesHandItemFilter(Interaction interaction, Player player) {
        InteractionHand interactionHand = interaction.getInteractionHand();

        ItemStack mainHandItem = player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND);
        ItemStack offHandItem = player.getItemInHand(net.minecraft.world.InteractionHand.OFF_HAND);

        if (interactionHand == null) {
            return mainHandItem.isEmpty() && offHandItem.isEmpty();
        }

        ItemStack interactionItem = interactionHand.getItemStack();

        if (interactionItem.isEmpty()) {
            switch (interactionHand.getHandType()) {
                case ANY_HAND:
                    return mainHandItem.isEmpty() && offHandItem.isEmpty();
                case MAIN_HAND:
                    return mainHandItem.isEmpty();
                case OFF_HAND:
                    return offHandItem.isEmpty();
                default:
                    return false;
            }
        } else {
            return switch (interactionHand.getHandType()) {
                case ANY_HAND ->
                        (matchesItem(mainHandItem, interactionItem) && matchesNBT(mainHandItem, interactionHand.getNbt())) ||
                                (matchesItem(offHandItem, interactionItem) && matchesNBT(offHandItem, interactionHand.getNbt()));
                case MAIN_HAND ->
                        matchesItem(mainHandItem, interactionItem) && matchesNBT(mainHandItem, interactionHand.getNbt());
                case OFF_HAND ->
                        matchesItem(offHandItem, interactionItem) && matchesNBT(offHandItem, interactionHand.getNbt());
                default -> false;
            };
        }
    }

    // Inner work ( Util )

    private boolean matchesBlockState(BlockState state, List<StateEntry<?>> stateEntries) {
        for (StateEntry<?> entry : stateEntries) {
            if (!state.getValues().containsKey(entry.getProperty()) ||
                    !state.getValue(entry.getProperty()).equals(entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesItem(ItemStack itemStack, ItemStack requiredItemStack) {
        return itemStack.is(requiredItemStack.getItem());
    }

    private boolean matchesNBT(ItemStack itemStack, CompoundTag requiredNBT) {
        if (requiredNBT.isEmpty()) { return true; }

        CompoundTag itemNBT = itemStack.getTag();
        if (itemNBT == null) { return false; }

        return TagHelper.checkNBTs(itemNBT, requiredNBT, true);
    }
}
