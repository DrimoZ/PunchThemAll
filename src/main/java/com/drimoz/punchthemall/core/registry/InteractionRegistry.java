package com.drimoz.punchthemall.core.registry;

import com.drimoz.punchthemall.PTAConfig;
import com.drimoz.punchthemall.core.codec.InteractionSpec;
import com.drimoz.punchthemall.core.codec.InteractionSpecResolver;
import com.drimoz.punchthemall.core.model.classes.PtaBlock;
import com.drimoz.punchthemall.core.model.classes.PtaHand;
import com.drimoz.punchthemall.core.model.classes.PtaInteraction;
import com.drimoz.punchthemall.core.model.classes.PtaNbtPredicate;
import com.drimoz.punchthemall.core.model.enums.PtaTypeEnum;
import com.drimoz.punchthemall.core.model.records.PtaStateRecord;
import com.drimoz.punchthemall.core.util.ItemView;
import com.drimoz.punchthemall.core.util.PTALoggers;
import com.drimoz.punchthemall.core.util.TagHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.*;

public class InteractionRegistry {

    private static final InteractionRegistry INSTANCE = new InteractionRegistry();
    private final Map<ResourceLocation, PtaInteraction> interactions = new HashMap<>();

    // Runtime indexes: candidates keyed by resolved click type and concrete target, rebuilt lazily.
    private final Map<PtaTypeEnum, Map<Block, List<PtaInteraction>>> blockIndex = new EnumMap<>(PtaTypeEnum.class);
    private final Map<PtaTypeEnum, Map<Fluid, List<PtaInteraction>>> fluidIndex = new EnumMap<>(PtaTypeEnum.class);
    private final Map<PtaTypeEnum, List<PtaInteraction>> airIndex = new EnumMap<>(PtaTypeEnum.class);
    private boolean indexDirty = true;

    private InteractionRegistry() {}

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

    /**
     * Rebuild the runtime interactions by resolving every {@link InteractionSpec} in the
     * {@code pta:interaction} datapack registry against the given provider. Called on both the server
     * (after datapack load) and the client (after the registry is synchronised), so gameplay and JEI
     * stay consistent everywhere.
     */
    public void rebuildFrom(HolderLookup.Provider registries) {
        clearInteractions();

        registries.lookup(PtaRegistries.INTERACTION).ifPresent(lookup ->
                lookup.listElements().forEach(holder -> {
                    ResourceLocation id = holder.key().location();
                    InteractionSpec spec = holder.value();

                    if (spec.schemaVersion() < 2) {
                        PTALoggers.error(RegistryConstants.INCORRECT_FORMAT + " - " + id
                                + " - schema_version " + spec.schemaVersion() + " is not supported; requires schema_version 2");
                        return;
                    }
                    if (!spec.enabled()) {
                        return;
                    }

                    PtaInteraction interaction = InteractionSpecResolver.resolve(id, spec, registries);
                    if (interaction != null) {
                        addInteraction(interaction);
                        if (PTAConfig.DEBUG.logLoadedInteractions.get()) {
                            PTALoggers.info("Loaded PunchThemAll interaction " + id);
                        }
                    }
                })
        );

        PTALoggers.info("Loaded " + interactions.size() + " interaction(s)");
    }

    public PtaInteraction getInteractionById(ResourceLocation id) {
        return interactions.get(id);
    }

    public Set<PtaInteraction> getFilteredInteractions(PtaTypeEnum interactionType, boolean clickOnBlock, Player player, BlockPos pos, Level level) {
        Set<PtaInteraction> filteredInteractions = new HashSet<>();

        PtaTypeEnum eventType = PtaTypeEnum.getTypeFromEvent(interactionType, player.isShiftKeyDown());

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
            } else if (ptaBlock.isBlock()) {
                Map<Block, List<PtaInteraction>> byBlock = blockIndex.computeIfAbsent(type, t -> new HashMap<>());
                for (Block block : ptaBlock.getBlockSet()) {
                    byBlock.computeIfAbsent(block, b -> new ArrayList<>()).add(interaction);
                }
            } else {
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
        return passesInteractionTypeFilter(interaction, eventType) &&
                passesBiomeAndDimensionFilter(interaction, level, pos) &&
                interaction.getConditions().matches(level, player, pos) &&
                passesAirOrBlockFilter(interaction, clickOnBlock) &&
                passesBlockStateFilter(interaction, clickOnBlock, pos, level) &&
                passesBlockEntityNBTFilter(interaction, clickOnBlock, pos, level) &&
                passesHandItemFilter(interaction, player);
    }

    private boolean passesInteractionTypeFilter(PtaInteraction interaction, PtaTypeEnum eventType) {
        return interaction.getType().equals(eventType);
    }

    private boolean passesBiomeAndDimensionFilter(PtaInteraction interaction, Level level, BlockPos pos) {
        if (interaction.hasBiomeWhiteList()) {
            return biomeOrDimensionMatches(interaction.getBiomeWhitelist(), level, pos);
        }
        if (interaction.hasBiomeBlackList()) {
            return !biomeOrDimensionMatches(interaction.getBiomeBlackList(), level, pos);
        }
        return true;
    }

    // Matches an entry set against the current dimension/biome. A '#' prefix means a biome tag.
    private boolean biomeOrDimensionMatches(Set<String> entries, Level level, BlockPos pos) {
        String dimensionId = level.dimension().location().toString();
        var biomeHolder = level.getBiome(pos);
        String biomeId = biomeHolder.unwrapKey().map(key -> key.location().toString()).orElse("");

        for (String entry : entries) {
            if (!entry.isEmpty() && entry.charAt(0) == '#') {
                TagKey<Biome> tag = TagKey.create(Registries.BIOME, ResourceLocation.parse(entry.substring(1)));
                if (biomeHolder.is(tag)) return true;
            } else if (entry.equals(dimensionId) || entry.equals(biomeId)) {
                return true;
            }
        }
        return false;
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

        if (!ptaBlock.isBlockFromSet(block) && !ptaBlock.isFluidFromSet(fluid)) {
            return false;
        }

        for (PtaStateRecord<?> stateRecord : ptaBlock.getStateWhiteList()) {
            if (!matchesState(blockState, fluidState, stateRecord, ptaBlock.isBlock())) {
                return false;
            }
        }

        for (PtaStateRecord<?> stateRecord : ptaBlock.getStateBlackList()) {
            if (matchesState(blockState, fluidState, stateRecord, ptaBlock.isBlock())) {
                return false;
            }
        }

        return true;
    }

    private boolean passesBlockEntityNBTFilter(PtaInteraction interaction, boolean clickOnBlock, BlockPos pos, Level level) {
        PtaBlock ptaBlock = interaction.getBlock();
        if (!clickOnBlock || (!ptaBlock.hasNbtBlackList() && !ptaBlock.hasNbtWhiteList() && !ptaBlock.hasNbtPredicates())) {
            return true;
        }

        BlockEntity worldBlockEntity = level.getBlockEntity(pos);
        if (worldBlockEntity == null) return false;

        // Block entities still serialise to a CompoundTag (needs the registry provider in 1.21).
        CompoundTag worldBlockEntityTag = worldBlockEntity.saveWithoutMetadata(level.registryAccess());

        boolean passesWhiteList = true, passesBlackList = true;
        if (ptaBlock.hasNbtWhiteList())
            passesWhiteList = TagHelper.containsRequiredTagsWithRange(worldBlockEntityTag, ptaBlock.getNbtWhiteList());

        if (ptaBlock.hasNbtBlackList())
            passesBlackList = TagHelper.containsRequiredTagsWithRangeBlacklist(worldBlockEntityTag, ptaBlock.getNbtBlackList());

        boolean passesPredicates = !ptaBlock.hasNbtPredicates() || matchesPredicates(worldBlockEntityTag, ptaBlock.getNbtPredicates());

        return passesWhiteList && passesBlackList && passesPredicates;
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

            if (hand.hasNbtPredicates()) {
                matchesMainHand = matchesMainHand && matchesPredicates(ItemView.of(mainHandItem), hand.getNbtPredicates());
                matchesOffHand = matchesOffHand && matchesPredicates(ItemView.of(offHandItem), hand.getNbtPredicates());
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

    // Item NBT is matched against PTA's stable ItemView (so the authoring format is version-stable).
    private boolean matchesNBTWhitelist(ItemStack itemStack, CompoundTag nbtWhitelist) {
        return TagHelper.containsRequiredTagsWithRange(ItemView.of(itemStack), nbtWhitelist);
    }

    private boolean matchesNBTBlacklist(ItemStack itemStack, CompoundTag nbtBlacklist) {
        return TagHelper.containsRequiredTagsWithRangeBlacklist(ItemView.of(itemStack), nbtBlacklist);
    }

    private boolean matchesPredicates(CompoundTag tag, List<PtaNbtPredicate> predicates) {
        CompoundTag effective = tag == null ? new CompoundTag() : tag;
        for (PtaNbtPredicate predicate : predicates) {
            if (!predicate.matches(effective)) {
                return false;
            }
        }
        return true;
    }

    public int getJEIRowCount() {
        int maxRows = 0;
        for (PtaInteraction interaction : interactions.values()) {
            maxRows = Math.max(maxRows, interaction.getRewards().getJeiRowCount());
        }
        return maxRows;
    }
}
