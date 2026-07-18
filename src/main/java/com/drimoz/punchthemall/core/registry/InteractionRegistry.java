package com.drimoz.punchthemall.core.registry;

import com.drimoz.punchthemall.core.model.classes.PtaBlock;
import com.drimoz.punchthemall.core.model.classes.PtaHand;
import com.drimoz.punchthemall.core.model.classes.PtaInteraction;
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
import net.minecraftforge.common.util.FakePlayer;

import java.util.*;

public class InteractionRegistry {

    // Private properties

    private static final InteractionRegistry INSTANCE = new InteractionRegistry();
    private final Map<ResourceLocation, PtaInteraction> interactions = new HashMap<>();

    // Runtime indexes: candidates keyed by resolved click type and concrete target.
    // Rebuilt lazily after any mutation so a click filters only the small matching bucket
    // instead of scanning every interaction.
    private final Map<PtaTypeEnum, Map<Block, List<PtaInteraction>>> blockIndex = new EnumMap<>(PtaTypeEnum.class);
    private final Map<PtaTypeEnum, Map<Fluid, List<PtaInteraction>>> fluidIndex = new EnumMap<>(PtaTypeEnum.class);
    private final Map<PtaTypeEnum, List<PtaInteraction>> airIndex = new EnumMap<>(PtaTypeEnum.class);
    private boolean indexDirty = true;

    // Life cycle

    private InteractionRegistry() {}

    // Interface

    public static InteractionRegistry getInstance() {
        return INSTANCE;
    }

    public void clearInteractions() {
        this.interactions.clear();
        this.indexDirty = true;
    }

    public Map<ResourceLocation, PtaInteraction> getInteractions() {
        return interactions;
    }

    public void addInteraction(PtaInteraction interaction) {
        interactions.put(interaction.getId(), interaction);
        this.indexDirty = true;
    }

    public PtaInteraction getInteractionById(ResourceLocation id) {
        return interactions.get(id);
    }

    public Set<PtaInteraction> getFilteredInteractions(PtaTypeEnum interactionType, boolean clickOnBlock, Player player, BlockPos pos, Level level) {
        Set<PtaInteraction> filteredInteractions = new HashSet<>();

        PtaTypeEnum eventType = PtaTypeEnum.getTypeFromEvent(interactionType, player.isShiftKeyDown());

        // Prefilter to the small bucket of candidates matching this click type and concrete target.
        // The full per-interaction filters still run below, so semantics are unchanged.
        for (PtaInteraction interaction : getCandidates(eventType, clickOnBlock, pos, level)) {
            if (!passesInteractionFilters(interaction, eventType, clickOnBlock, player, pos, level)) {
                continue;
            }
            filteredInteractions.add(interaction);
        }

        return filteredInteractions;
    }

    private Collection<PtaInteraction> getCandidates(PtaTypeEnum eventType, boolean clickOnBlock, BlockPos pos, Level level) {
        rebuildIndexIfNeeded();

        if (!clickOnBlock) {
            return airIndex.getOrDefault(eventType, List.of());
        }

        Block block = level.getBlockState(pos).getBlock();
        Fluid fluid = level.getFluidState(pos).getType();

        List<PtaInteraction> byBlock = blockIndex.getOrDefault(eventType, Map.of()).get(block);
        List<PtaInteraction> byFluid = fluidIndex.getOrDefault(eventType, Map.of()).get(fluid);

        if (byFluid == null || byFluid.isEmpty()) {
            return byBlock == null ? List.of() : byBlock;
        }
        if (byBlock == null || byBlock.isEmpty()) {
            return byFluid;
        }

        // A waterlogged block can match both a block and a fluid interaction; merge without duplicates.
        Set<PtaInteraction> merged = new LinkedHashSet<>(byBlock);
        merged.addAll(byFluid);
        return merged;
    }

    private void rebuildIndexIfNeeded() {
        if (!indexDirty) return;

        blockIndex.clear();
        fluidIndex.clear();
        airIndex.clear();

        for (PtaInteraction interaction : interactions.values()) {
            PtaTypeEnum type = interaction.getType();
            PtaBlock ptaBlock = interaction.getBlock();

            if (ptaBlock.isAir()) {
                airIndex.computeIfAbsent(type, t -> new ArrayList<>()).add(interaction);
            }
            else if (ptaBlock.isBlock()) {
                Map<Block, List<PtaInteraction>> byBlock = blockIndex.computeIfAbsent(type, t -> new HashMap<>());
                for (Block block : ptaBlock.getBlockSet()) {
                    byBlock.computeIfAbsent(block, b -> new ArrayList<>()).add(interaction);
                }
            }
            else {
                Map<Fluid, List<PtaInteraction>> byFluid = fluidIndex.computeIfAbsent(type, t -> new HashMap<>());
                for (Fluid fluid : ptaBlock.getFluidSet()) {
                    byFluid.computeIfAbsent(fluid, f -> new ArrayList<>()).add(interaction);
                }
            }
        }

        indexDirty = false;
    }



    // Inner work ( Interaction Filter )

    private boolean passesInteractionFilters(
            PtaInteraction interaction, PtaTypeEnum eventType, boolean clickOnBlock,
            Player player, BlockPos pos, Level level
    ) {

        // PTALoggers.info("=================================");
        // PTALoggers.info("Interaction : " + interaction.getId().getPath());
        // PTALoggers.info("passesInteractionTypeFilter : " + passesInteractionTypeFilter(interaction, eventType));
        // PTALoggers.info("passesBiomeAndDimensionFilter : " + passesBiomeAndDimensionFilter(interaction, level, pos));
        // PTALoggers.info("passesAirOrBlockFilter : " + passesAirOrBlockFilter(interaction, clickOnBlock));
        // PTALoggers.info("passesBlockStateFilter : " + passesBlockStateFilter(interaction, clickOnBlock, pos, level));
        // PTALoggers.info("passesBlockEntityNBTFilter : " + passesBlockEntityNBTFilter(interaction, clickOnBlock, pos, level));
        // PTALoggers.info("passesHandItemFilter : " + passesHandItemFilter(interaction, player));

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
        // Guard against unregistered biome holders (custom worldgen): treat as "no biome id".
        String playerBiomeId = level.getBiome(pos).unwrapKey().map(key -> key.location().toString()).orElse("");

        // Check whitelist: only allow when the current dimension or biome is listed.
        if (interaction.hasBiomeWhiteList()) {
            return interaction.getBiomeWhitelist().contains(playerDimensionId) || interaction.getBiomeWhitelist().contains(playerBiomeId);
        }

        // Check blacklist: forbid when the current dimension or biome is listed.
        if (interaction.hasBiomeBlackList()) {
            return !(interaction.getBiomeBlackList().contains(playerDimensionId) || interaction.getBiomeBlackList().contains(playerBiomeId));
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

        if (player instanceof FakePlayer) {
                offHandItem = mainHandItem;
        }

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
