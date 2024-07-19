package com.drimoz.punchthemall.core.registry;

import com.drimoz.punchthemall.core.model.classes.PtaBlock;
import com.drimoz.punchthemall.core.model.classes.PtaHand;
import com.drimoz.punchthemall.core.model.classes.PtaInteraction;
import com.drimoz.punchthemall.core.model.enums.PtaHandEnum;
import com.drimoz.punchthemall.core.model.enums.PtaTypeEnum;
import com.drimoz.punchthemall.core.model.records.PtaStateRecord;
import com.drimoz.punchthemall.core.util.PTALoggers;
import com.drimoz.punchthemall.core.util.TagHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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
    private final Map<ResourceLocation, PtaInteraction> interactions = new HashMap<>();

    // Life cycle

    private InteractionRegistry() {}

    // Interface

    public static InteractionRegistry getInstance() {
        return INSTANCE;
    }

    public Map<ResourceLocation, PtaInteraction> getInteractions() {
        return interactions;
    }

    public void addInteraction(PtaInteraction interaction) {
        interactions.put(interaction.getId(), interaction);
    }

    public PtaInteraction getInteractionById(ResourceLocation id) {
        return interactions.get(id);
    }

    public Set<PtaInteraction> getFilteredInteractions(PtaTypeEnum interactionType, boolean clickOnBlock, Player player, BlockPos pos, Level level) {
        Set<PtaInteraction> filteredInteractions = new HashSet<>();

        PtaTypeEnum eventType = PtaTypeEnum.getTypeFromEvent(interactionType, player.isShiftKeyDown());

        for (PtaInteraction interaction : interactions.values()) {
            if (!passesInteractionFilters(interaction, eventType, clickOnBlock, player, pos, level)) {
                continue;
            }
            filteredInteractions.add(interaction);
        }

        return filteredInteractions;
    }



    // Inner work ( Interaction Filter )

    private boolean passesInteractionFilters(
            PtaInteraction interaction, PtaTypeEnum eventType, boolean clickOnBlock,
            Player player, BlockPos pos, Level level
    ) {
        // PTALoggers.error("passesInteractionTypeFilter : " + passesInteractionTypeFilter(interaction, eventType));
        // PTALoggers.error("passesBiomeAndDimensionFilter : " + passesBiomeAndDimensionFilter(interaction, level, pos));
        // PTALoggers.error("passesAirOrBlockFilter : " + passesAirOrBlockFilter(interaction, clickOnBlock));
        // PTALoggers.error("passesBlockStateFilter : " + passesBlockStateFilter(interaction, clickOnBlock, pos, level));
        // PTALoggers.error("passesBlockEntityNBTFilter : " + passesBlockEntityNBTFilter(interaction, clickOnBlock, pos, level));
        // PTALoggers.error("passesHandItemFilter : " + passesHandItemFilter(interaction, player));

        return passesInteractionTypeFilter(interaction, eventType) &&
                passesBiomeAndDimensionFilter(interaction, level, pos) &&
                passesAirOrBlockFilter(interaction, clickOnBlock) &&
                passesBlockStateFilter(interaction, clickOnBlock, pos, level) &&
                passesBlockEntityNBTFilter(interaction, clickOnBlock, pos, level) &&
                passesHandItemFilter(interaction, player);
    }

    private boolean passesInteractionTypeFilter(PtaInteraction interaction, PtaTypeEnum eventType) {
        return interaction.getType().equals(eventType);
    }

    private boolean passesBiomeAndDimensionFilter(PtaInteraction interaction, Level level, BlockPos pos) {
        String playerDimensionId = level.dimension().location().toString();
        String playerBiomeId = level.getBiome(pos).unwrapKey().get().location().toString();

        // Check whitelist
        if (interaction.hasBiomeWhiteList()) {
            return interaction.getBiomeWhitelist().contains(playerDimensionId) || interaction.getBiomeWhitelist().contains(playerBiomeId);
        }

        // Check blacklist
        if (interaction.hasBiomeBlackList()) {
            return interaction.getBiomeBlackList().contains(playerDimensionId) || interaction.getBiomeBlackList().contains(playerBiomeId);
        }

        return true;
    }

    private boolean passesAirOrBlockFilter(PtaInteraction interaction, boolean clickOnBlock) {
        return interaction.getBlock().isAir() == !clickOnBlock;
    }

    private boolean passesBlockStateFilter(PtaInteraction interaction, boolean clickOnBlock, BlockPos pos, Level level) {
        if (!clickOnBlock) {
            return true;
        }

        PtaBlock ptaBlock = interaction.getBlock();

        BlockState blockState = level.getBlockState(pos);
        FluidState fluidState = level.getFluidState(pos);

        Block block = blockState.getBlock();
        Fluid fluid = fluidState.getType();

        // Check if the block or fluid matches the interaction's whitelist or blacklist
        if (!ptaBlock.isBlockFromSet(block) && !ptaBlock.isFluidFromSet(fluid)) {
            return false;
        }

        // Validate whitelist states
        for (PtaStateRecord<?> stateRecord : ptaBlock.getStateWhiteList()) {
            if (!matchesState(blockState, fluidState, stateRecord, ptaBlock.isBlock())) {
                return false;
            }
        }

        // Validate blacklist states
        for (PtaStateRecord<?> stateRecord : ptaBlock.getStateBlackList()) {
            if (matchesState(blockState, fluidState, stateRecord, ptaBlock.isBlock())) {
                return false;
            }
        }

        return true;
    }

    private boolean passesBlockEntityNBTFilter(PtaInteraction interaction, boolean clickOnBlock, BlockPos pos, Level level) {
        if (!clickOnBlock || (!interaction.getBlock().hasNbtBlackList() && !interaction.getBlock().hasNbtWhiteList())) {
            return true;
        }

        BlockEntity worldBlockEntity = level.getBlockEntity(pos);
        if (worldBlockEntity == null) return false;

        CompoundTag worldBlockEntityTag = worldBlockEntity.serializeNBT();

        boolean passesWhiteList = true, passesBlackList = true;
        if (interaction.getBlock().hasNbtWhiteList())
            passesWhiteList = TagHelper.containsRequiredTagsWithRange(worldBlockEntityTag, interaction.getBlock().getNbtWhiteList());

        if (interaction.getBlock().hasNbtBlackList())
            passesBlackList = TagHelper.containsRequiredTagsWithRangeBlacklist(worldBlockEntityTag, interaction.getBlock().getNbtBlackList());

        return passesWhiteList && passesBlackList;
    }

    private boolean passesHandItemFilter(PtaInteraction interaction, Player player) {
        PtaHand hand = interaction.getHand();

        ItemStack mainHandItem = player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND);
        ItemStack offHandItem = player.getItemInHand(net.minecraft.world.InteractionHand.OFF_HAND);

        if (hand.isEmpty()) {
            return mainHandItem.isEmpty() && offHandItem.isEmpty();
        }

        if (hand.getItemSet().isEmpty()) {
            return switch (hand.getHand()) {
                case ANY_HAND -> mainHandItem.isEmpty() && offHandItem.isEmpty();
                case MAIN_HAND -> mainHandItem.isEmpty();
                case OFF_HAND -> offHandItem.isEmpty();
            };
        } else {
            boolean matchesMainHand = matchesItem(mainHandItem, hand.getItemSet());
            boolean matchesOffHand = matchesItem(offHandItem, hand.getItemSet());

            if (hand.hasNbtWhiteList()) {
                matchesMainHand = matchesMainHand && matchesNBTWhitelist(mainHandItem, hand.getNbtWhiteList());
                matchesOffHand = matchesOffHand && matchesNBTWhitelist(offHandItem, hand.getNbtWhiteList());
            }

            if (hand.hasNbtBlackList()) {
                matchesMainHand = matchesMainHand && matchesNBTBlacklist(mainHandItem, hand.getNbtBlackList());
                matchesOffHand = matchesOffHand && matchesNBTBlacklist(offHandItem, hand.getNbtBlackList());
            }

            return switch (hand.getHand()) {
                case ANY_HAND -> (matchesMainHand || matchesOffHand);
                case MAIN_HAND -> matchesMainHand;
                case OFF_HAND -> matchesOffHand;
            };
        }
    }

    private boolean matchesState(BlockState blockState, FluidState fluidState, PtaStateRecord<?> stateRecord, boolean isBlock) {
        if (isBlock) {
            return blockState.getProperties().contains(stateRecord.property())
                    && blockState.getValue(stateRecord.property()).equals(stateRecord.getValue());
        } else {
            return fluidState.getProperties().contains(stateRecord.property())
                    && fluidState.getValue(stateRecord.property()).equals(stateRecord.getValue());
        }
    }

    private boolean matchesItem(ItemStack itemStack, Set<Item> itemSet) {
        return itemSet.isEmpty() || itemSet.contains(itemStack.getItem());
    }

    private boolean matchesNBTWhitelist(ItemStack itemStack, CompoundTag nbtWhitelist) {
        return TagHelper.containsRequiredTagsWithRange(itemStack.getTag(), nbtWhitelist);
    }

    private boolean matchesNBTBlacklist(ItemStack itemStack, CompoundTag nbtBlacklist) {
        return TagHelper.containsRequiredTagsWithRangeBlacklist(itemStack.getTag(), nbtBlacklist);
    }

    public int getJEIRowCount() {
        int maxPool = 0;

        for (PtaInteraction interaction : interactions.values()) {
            if (maxPool < interaction.getPool().getTotalPoolSize())
                maxPool = interaction.getPool().getTotalPoolSize();
        }

        return (int) Math.ceil(maxPool / 9.0);
    }
}
