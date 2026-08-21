package com.drimoz.punchthemall.core.registry;

import com.drimoz.punchthemall.core.model.classes.PtaBlock;
import com.drimoz.punchthemall.core.model.classes.PtaHand;
import com.drimoz.punchthemall.core.model.classes.PtaInteraction;
import com.drimoz.punchthemall.core.model.classes.PtaNbtPredicate;
import com.drimoz.punchthemall.core.model.enums.PtaTypeEnum;
import com.drimoz.punchthemall.core.model.records.PtaStateRecord;
import com.drimoz.punchthemall.core.util.PTALoggers;
import com.drimoz.punchthemall.core.util.TagHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
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

    // Raw JSON source per interaction id, captured at load time. Used to synchronise the registry
    // to clients (S2C) so JEI shows the server's interactions on dedicated servers.
    private final Map<ResourceLocation, String> sources = new HashMap<>();

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
        this.sources.clear();
        this.indexDirty = true;
    }

    public Map<ResourceLocation, PtaInteraction> getInteractions() {
        return interactions;
    }

    public void addInteraction(PtaInteraction interaction) {
        addInteraction(interaction, null);
    }

    public void addInteraction(PtaInteraction interaction, String rawJson) {
        interactions.put(interaction.getId(), interaction);
        if (rawJson != null) {
            sources.put(interaction.getId(), rawJson);
        }
        this.indexDirty = true;
    }

    /** Raw JSON keyed by interaction id, for S2C synchronisation. */
    public Map<ResourceLocation, String> getSources() {
        return sources;
    }

    public PtaInteraction getInteractionById(ResourceLocation id) {
        return interactions.get(id);
    }

    /**
     * The interactions a click should run, in a stable order.
     *
     * <p>Ordered rather than a set because the caller stops at {@code max_matches_per_click} and
     * applies at most one transformation: with a hash set, which of several competing interactions
     * won varied between runs, and between machines.</p>
     */
    public List<PtaInteraction> getFilteredInteractions(PtaTypeEnum interactionType, boolean clickOnBlock, Player player, BlockPos pos, Level level) {
        return getFilteredInteractions(interactionType, clickOnBlock, player, pos, level, null);
    }

    /** @param face the clicked face, so neighbour conditions written in the face frame resolve. */
    public List<PtaInteraction> getFilteredInteractions(PtaTypeEnum interactionType, boolean clickOnBlock, Player player, BlockPos pos, Level level, Direction face) {
        List<PtaInteraction> filteredInteractions = new ArrayList<>();

        PtaTypeEnum eventType = PtaTypeEnum.getTypeFromEvent(interactionType, player.isShiftKeyDown());

        // Prefilter to the small bucket of candidates matching this click type and concrete target.
        // The full per-interaction filters still run below, so semantics are unchanged.
        for (PtaInteraction interaction : getCandidates(eventType, clickOnBlock, pos, level)) {
            if (!passesInteractionFilters(interaction, eventType, clickOnBlock, player, pos, level, face)) {
                continue;
            }
            filteredInteractions.add(interaction);
        }

        return filteredInteractions;
    }

    /**
     * Whether any air interaction answers a left click. The client uses this to decide whether a
     * left click on nothing is worth telling the server about, so swinging at air costs nothing for
     * a pack without one.
     */
    public boolean hasLeftClickAirInteraction() {
        rebuildIndexIfNeeded();
        return !airIndex.getOrDefault(PtaTypeEnum.LEFT_CLICK, List.of()).isEmpty()
                || !airIndex.getOrDefault(PtaTypeEnum.SHIFT_LEFT_CLICK, List.of()).isEmpty();
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

        // Sorted by id so the candidate lists — and therefore which interactions survive
        // max_matches_per_click and which one transforms the block — are the same on every run.
        // `interactions` is a HashMap keyed by ResourceLocation, so its own iteration order is not.
        List<PtaInteraction> ordered = interactions.values().stream()
                .sorted(Comparator.comparing(interaction -> interaction.getId().toString()))
                .toList();

        for (PtaInteraction interaction : ordered) {
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
            Player player, BlockPos pos, Level level, Direction face
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
                interaction.getConditions().matches(level, player, pos, face) &&
                passesAirOrBlockFilter(interaction, clickOnBlock) &&
                passesBlockStateFilter(interaction, clickOnBlock, pos, level) &&
                passesBlockEntityNBTFilter(interaction, clickOnBlock, pos, level) &&
                passesHandItemFilter(interaction, player);
    }

    private boolean passesInteractionTypeFilter(PtaInteraction interaction, PtaTypeEnum eventType) {
        return interaction.getType().equals(eventType);
    }

    private boolean passesBiomeAndDimensionFilter(PtaInteraction interaction, Level level, BlockPos pos) {
        // Whitelist: only allow when the current dimension or biome is listed.
        if (interaction.hasBiomeWhiteList()) {
            return biomeOrDimensionMatches(interaction.getBiomeWhitelist(), level, pos);
        }

        // Blacklist: forbid when the current dimension or biome is listed.
        if (interaction.hasBiomeBlackList()) {
            return !biomeOrDimensionMatches(interaction.getBiomeBlackList(), level, pos);
        }

        return true;
    }

    /**
     * Matches an entry set against the current dimension and biome. An entry starting with a hash is
     * a biome tag.
     *
     * <p>Entries used to be compared as plain strings, so a tag entry matched nothing and the
     * interaction silently never fired — while the resolver validated it as a well-formed tag and
     * said nothing.</p>
     */
    private boolean biomeOrDimensionMatches(Set<String> entries, Level level, BlockPos pos) {
        String dimensionId = level.dimension().location().toString();
        Holder<Biome> biomeHolder = level.getBiome(pos);
        // Guard against unregistered biome holders (custom worldgen): treat as "no biome id".
        String biomeId = biomeHolder.unwrapKey().map(key -> key.location().toString()).orElse("");

        for (String entry : entries) {
            if (!entry.isEmpty() && entry.charAt(0) == 0x23) {
                // tryParse, not the constructor: this runs on every click, and a malformed tag in
                // one file would otherwise throw for as long as the pack is installed.
                ResourceLocation tagId = ResourceLocation.tryParse(entry.substring(1));
                if (tagId == null) continue;
                if (biomeHolder.is(TagKey.create(Registries.BIOME, tagId))) return true;
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
        PtaBlock ptaBlock = interaction.getBlock();
        if (!clickOnBlock || (!ptaBlock.hasNbtBlackList() && !ptaBlock.hasNbtWhiteList() && !ptaBlock.hasNbtPredicates())) {
            return true;
        }

        BlockEntity worldBlockEntity = level.getBlockEntity(pos);
        if (worldBlockEntity == null) return false;

        CompoundTag worldBlockEntityTag = worldBlockEntity.serializeNBT();

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
                matchesMainHand = matchesMainHand && matchesPredicates(mainHandItem.getTag(), hand.getNbtPredicates());
                matchesOffHand = matchesOffHand && matchesPredicates(offHandItem.getTag(), hand.getNbtPredicates());
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

    // All typed nbt_predicates must hold against the given tag (missing tag -> use an empty compound).
    private boolean matchesPredicates(CompoundTag tag, List<PtaNbtPredicate> predicates) {
        CompoundTag effective = tag == null ? new CompoundTag() : tag;
        for (PtaNbtPredicate predicate : predicates) {
            if (!predicate.matches(effective)) {
                return false;
            }
        }
        return true;
    }

    /** Tallest drop grid across the interactions JEI actually shows (hidden ones are skipped). */
    public int getJEIRowCount() {
        int maxRows = 0;

        for (PtaInteraction interaction : interactions.values()) {
            if (interaction.isHidden()) continue;
            maxRows = Math.max(maxRows, interaction.getRewards().getJeiRowCount());
        }

        return maxRows;
    }

    /** The interactions JEI should display. Hidden ones still load and still fire. */
    public List<PtaInteraction> getVisibleInteractions() {
        return interactions.values().stream()
                .filter(interaction -> !interaction.isHidden())
                .toList();
    }
}
