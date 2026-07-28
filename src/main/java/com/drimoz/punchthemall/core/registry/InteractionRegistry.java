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

/**
 * The resolved interactions the game actually runs, plus the lookup indexes clicks go through.
 *
 * <p>State is held in one immutable {@link Snapshot} behind a {@code volatile} field. A reload
 * publishes a fresh snapshot in a single write instead of clearing and refilling shared maps: in
 * singleplayer the server thread (datapack reload) and the client thread (sync payload) both rebuild
 * this same singleton while the server thread is reading it on every click, and a half-filled
 * {@code HashMap} under that pattern fails rarely and unreproducibly.</p>
 */
public class InteractionRegistry {

    private static final InteractionRegistry INSTANCE = new InteractionRegistry();

    private volatile Snapshot snapshot = Snapshot.EMPTY;

    private InteractionRegistry() {}

    public static InteractionRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Everything derived from one set of interactions, built once and never mutated. Candidate lists
     * keep the id order used when building, so which interactions a click sees — and therefore which
     * ones survive {@code max_matches_per_click} — is the same on every run and on every machine.
     */
    private record Snapshot(
            Map<ResourceLocation, PtaInteraction> interactions,
            Map<PtaTypeEnum, Map<Block, List<PtaInteraction>>> blockIndex,
            Map<PtaTypeEnum, Map<Fluid, List<PtaInteraction>>> fluidIndex,
            Map<PtaTypeEnum, List<PtaInteraction>> airIndex
    ) {
        static final Snapshot EMPTY = new Snapshot(Map.of(), Map.of(), Map.of(), Map.of());

        static Snapshot of(List<PtaInteraction> resolved) {
            Map<ResourceLocation, PtaInteraction> interactions = new LinkedHashMap<>();
            Map<PtaTypeEnum, Map<Block, List<PtaInteraction>>> blockIndex = new EnumMap<>(PtaTypeEnum.class);
            Map<PtaTypeEnum, Map<Fluid, List<PtaInteraction>>> fluidIndex = new EnumMap<>(PtaTypeEnum.class);
            Map<PtaTypeEnum, List<PtaInteraction>> airIndex = new EnumMap<>(PtaTypeEnum.class);

            for (PtaInteraction interaction : resolved) {
                interactions.put(interaction.getId(), interaction);

                PtaTypeEnum type = interaction.getType();
                PtaBlock target = interaction.getBlock();

                if (target.isAir()) {
                    airIndex.computeIfAbsent(type, t -> new ArrayList<>()).add(interaction);
                } else if (target.isBlock()) {
                    Map<Block, List<PtaInteraction>> byBlock = blockIndex.computeIfAbsent(type, t -> new HashMap<>());
                    for (Block block : target.getBlockSet()) {
                        byBlock.computeIfAbsent(block, b -> new ArrayList<>()).add(interaction);
                    }
                } else {
                    Map<Fluid, List<PtaInteraction>> byFluid = fluidIndex.computeIfAbsent(type, t -> new HashMap<>());
                    for (Fluid fluid : target.getFluidSet()) {
                        byFluid.computeIfAbsent(fluid, f -> new ArrayList<>()).add(interaction);
                    }
                }
            }

            return new Snapshot(
                    Collections.unmodifiableMap(interactions),
                    deepFreezeKeyed(blockIndex),
                    deepFreezeKeyed(fluidIndex),
                    freezeLists(airIndex)
            );
        }

        private static <K> Map<PtaTypeEnum, Map<K, List<PtaInteraction>>> deepFreezeKeyed(
                Map<PtaTypeEnum, Map<K, List<PtaInteraction>>> source
        ) {
            Map<PtaTypeEnum, Map<K, List<PtaInteraction>>> frozen = new EnumMap<>(PtaTypeEnum.class);
            source.forEach((type, byKey) -> {
                Map<K, List<PtaInteraction>> inner = new HashMap<>();
                byKey.forEach((key, list) -> inner.put(key, List.copyOf(list)));
                frozen.put(type, Collections.unmodifiableMap(inner));
            });
            return Collections.unmodifiableMap(frozen);
        }

        private static Map<PtaTypeEnum, List<PtaInteraction>> freezeLists(Map<PtaTypeEnum, List<PtaInteraction>> source) {
            Map<PtaTypeEnum, List<PtaInteraction>> frozen = new EnumMap<>(PtaTypeEnum.class);
            source.forEach((type, list) -> frozen.put(type, List.copyOf(list)));
            return Collections.unmodifiableMap(frozen);
        }
    }

    /** Drop every interaction. Used when a client leaves a server, so the viewers do not show stale data. */
    public void clearInteractions() {
        this.snapshot = Snapshot.EMPTY;
    }

    /** The loaded interactions, in id order. Unmodifiable. */
    public Map<ResourceLocation, PtaInteraction> getInteractions() {
        return snapshot.interactions();
    }

    /**
     * Rebuild the runtime interactions by resolving each {@link InteractionSpec} against the given
     * provider. The server calls this from the reload listener, the client from the sync payload
     * handler, so both sides resolve against their own registries and gameplay matches what the
     * viewers display.
     *
     * <p>Specs are sorted by id first, so the resulting candidate order is stable. A spec that fails
     * to resolve is reported and skipped: one unlucky file must not cost the pack every other
     * interaction, and this runs inside datapack loading where an exception aborts the whole reload.</p>
     */
    public void rebuildFrom(Map<ResourceLocation, InteractionSpec> specs, HolderLookup.Provider registries) {
        List<PtaInteraction> resolved = new ArrayList<>();

        specs.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    ResourceLocation id = entry.getKey();
                    InteractionSpec spec = entry.getValue();

                    if (spec.schemaVersion() < 2) {
                        PTALoggers.error(RegistryConstants.INCORRECT_FORMAT + " - " + id
                                + " - schema_version " + spec.schemaVersion() + " is not supported; requires schema_version 2");
                        return;
                    }
                    if (!spec.enabled()) {
                        return;
                    }

                    try {
                        PtaInteraction interaction = InteractionSpecResolver.resolve(id, spec, registries);
                        if (interaction != null) {
                            resolved.add(interaction);
                            if (PTAConfig.valueOrDefault(PTAConfig.DEBUG.logLoadedInteractions)) {
                                PTALoggers.info("Loaded PunchThemAll interaction " + id);
                            }
                        }
                    } catch (RuntimeException e) {
                        PTALoggers.error(RegistryConstants.INCORRECT_FORMAT + " - " + id
                                + " - could not be resolved and was skipped: " + e);
                    }
                });

        this.snapshot = Snapshot.of(resolved);

        PTALoggers.info("Loaded " + resolved.size() + " interaction(s)");
    }

    public PtaInteraction getInteractionById(ResourceLocation id) {
        return snapshot.interactions().get(id);
    }

    /**
     * The interactions a click should run, in a stable order. Ordered rather than a set because the
     * caller stops at {@code max_matches_per_click} and applies at most one transformation, so an
     * arbitrary iteration order would make it unpredictable which of several matches wins.
     */
    public List<PtaInteraction> getFilteredInteractions(PtaTypeEnum interactionType, boolean clickOnBlock, Player player, BlockPos pos, Level level) {
        PtaTypeEnum eventType = PtaTypeEnum.getTypeFromEvent(interactionType, player.isShiftKeyDown());

        List<PtaInteraction> matches = new ArrayList<>();
        for (PtaInteraction interaction : getCandidates(eventType, clickOnBlock, pos, level)) {
            if (passesInteractionFilters(interaction, eventType, clickOnBlock, player, pos, level)) {
                matches.add(interaction);
            }
        }
        return matches;
    }

    /**
     * Whether any air interaction answers a left click. The client uses this to decide if a
     * left-click-on-nothing is worth telling the server about, so packs without such an interaction
     * cost nothing per swing.
     */
    public boolean hasLeftClickAirInteraction() {
        Map<PtaTypeEnum, List<PtaInteraction>> airIndex = snapshot.airIndex();
        return !airIndex.getOrDefault(PtaTypeEnum.LEFT_CLICK, List.of()).isEmpty()
                || !airIndex.getOrDefault(PtaTypeEnum.SHIFT_LEFT_CLICK, List.of()).isEmpty();
    }

    private Collection<PtaInteraction> getCandidates(PtaTypeEnum eventType, boolean clickOnBlock, BlockPos pos, Level level) {
        Snapshot current = snapshot;

        if (!clickOnBlock) {
            return current.airIndex().getOrDefault(eventType, List.of());
        }

        Block block = level.getBlockState(pos).getBlock();
        Fluid fluid = level.getFluidState(pos).getType();

        List<PtaInteraction> byBlock = current.blockIndex().getOrDefault(eventType, Map.of()).get(block);
        List<PtaInteraction> byFluid = current.fluidIndex().getOrDefault(eventType, Map.of()).get(fluid);

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
                // tryParse, not parse: this runs on every click, and a malformed tag in one file
                // would otherwise throw for as long as the pack is installed.
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

    /** Tallest drop grid across the interactions the viewers actually show (hidden ones are skipped). */
    public int getJEIRowCount() {
        int maxRows = 0;
        for (PtaInteraction interaction : snapshot.interactions().values()) {
            if (interaction.isHidden()) continue;
            maxRows = Math.max(maxRows, interaction.getRewards().getJeiRowCount());
        }
        return maxRows;
    }

    /** The interactions the recipe viewers should display, in id order. */
    public List<PtaInteraction> getVisibleInteractions() {
        return snapshot.interactions().values().stream()
                .filter(interaction -> !interaction.isHidden())
                .toList();
    }
}
