package com.drimoz.punchthemall.core.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Structural spec for the schema_version 2 interaction format.
 *
 * <p>These records are a strict, validated mirror of the JSON. They hold only raw values
 * (ids/tags as strings, ranges, SNBT compounds); {@code InteractionSpecResolver} turns them
 * into the existing runtime model so gameplay is unchanged. See {@code docs/interaction-format.md}.</p>
 */
public record InteractionSpec(
        int schemaVersion,
        boolean enabled,
        boolean hidden,
        String type,
        Optional<HandSpec> hand,
        Optional<TargetSpec> target,
        List<TransformationSpec> transformation,
        Optional<RewardsSpec> rewards,
        Optional<CostsSpec> costs,
        Optional<ConditionsSpec> conditions,
        List<EffectSpec> effects,
        Optional<String> sound,
        Optional<String> particles
) {

    public static final Codec<InteractionSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("schema_version", 2).forGetter(InteractionSpec::schemaVersion),
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(InteractionSpec::enabled),
            // Loads and fires as usual, but JEI/EMI leave it out. Distinct from `enabled: false`,
            // which does not load at all.
            Codec.BOOL.optionalFieldOf("hidden", false).forGetter(InteractionSpec::hidden),
            Codec.STRING.fieldOf("type").forGetter(InteractionSpec::type),
            HandSpec.CODEC.optionalFieldOf("hand").forGetter(InteractionSpec::hand),
            TargetSpec.CODEC.optionalFieldOf("target").forGetter(InteractionSpec::target),
            // One transformation or a list of them; a single object stays the short form on the way
            // back out, so files that never asked for more than one are untouched by the change.
            PtaCodecs.objectOrList(TransformationSpec.CODEC).optionalFieldOf("transformation", List.of()).forGetter(InteractionSpec::transformation),
            RewardsSpec.CODEC.optionalFieldOf("rewards").forGetter(InteractionSpec::rewards),
            CostsSpec.CODEC.optionalFieldOf("costs").forGetter(InteractionSpec::costs),
            ConditionsSpec.CODEC.optionalFieldOf("conditions").forGetter(InteractionSpec::conditions),
            EffectSpec.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(InteractionSpec::effects),
            Codec.STRING.optionalFieldOf("sound").forGetter(InteractionSpec::sound),
            Codec.STRING.optionalFieldOf("particles").forGetter(InteractionSpec::particles)
    ).apply(instance, InteractionSpec::new));

    // A pair of optional SNBT compounds, reused for hand items and block entities.
    public record NbtPairSpec(Optional<CompoundTag> whitelist, Optional<CompoundTag> blacklist) {
        public static final Codec<NbtPairSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                PtaCodecs.SNBT.optionalFieldOf("whitelist").forGetter(NbtPairSpec::whitelist),
                PtaCodecs.SNBT.optionalFieldOf("blacklist").forGetter(NbtPairSpec::blacklist)
        ).apply(instance, NbtPairSpec::new));

        public static final NbtPairSpec EMPTY = new NbtPairSpec(Optional.empty(), Optional.empty());
    }

    // A typed NBT predicate (§5.3): path + optional [min,max] range + optional "where" filter.
    public record NbtPredicateSpec(String path, Optional<List<Integer>> intRange, Optional<CompoundTag> where) {
        public static final Codec<NbtPredicateSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("path").forGetter(NbtPredicateSpec::path),
                Codec.INT.listOf().optionalFieldOf("int_range").forGetter(NbtPredicateSpec::intRange),
                PtaCodecs.SNBT.optionalFieldOf("where").forGetter(NbtPredicateSpec::where)
        ).apply(instance, NbtPredicateSpec::new));
    }

    // A pair of optional state maps (property name -> expected value), reused for whitelist/blacklist.
    public record StatePairSpec(Map<String, String> whitelist, Map<String, String> blacklist) {
        private static final Codec<Map<String, String>> STATE_MAP = Codec.unboundedMap(Codec.STRING, PtaCodecs.SCALAR_STRING);

        public static final Codec<StatePairSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                STATE_MAP.optionalFieldOf("whitelist", Map.of()).forGetter(StatePairSpec::whitelist),
                STATE_MAP.optionalFieldOf("blacklist", Map.of()).forGetter(StatePairSpec::blacklist)
        ).apply(instance, StatePairSpec::new));

        public static final StatePairSpec EMPTY = new StatePairSpec(Map.of(), Map.of());
    }

    // How a held item is spent on a successful interaction.
    //
    // `chance` and `count` are independent: `chance` decides *whether* anything is spent, `count`
    // decides *how much* once that roll succeeds. `chance: 0.33` with `count: {min:3, max:5}` means
    // a one-in-three chance of spending three to five.
    public record ConsumeSpec(String mode, double chance, CountSpec count) {
        public static final Codec<ConsumeSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.optionalFieldOf("mode", "none").forGetter(ConsumeSpec::mode),
                Codec.DOUBLE.optionalFieldOf("chance", 1.0D).forGetter(ConsumeSpec::chance),
                CountSpec.CODEC.optionalFieldOf("count", CountSpec.exact(1)).forGetter(ConsumeSpec::count)
        ).apply(instance, ConsumeSpec::new));

        public static final ConsumeSpec NONE = new ConsumeSpec("none", 1.0D, CountSpec.exact(1));
    }

    public record HandSpec(
            String hand,
            List<String> match,
            ConsumeSpec consume,
            NbtPairSpec nbt,
            List<NbtPredicateSpec> nbtPredicates
    ) {
        public static final Codec<HandSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.optionalFieldOf("hand", "any").forGetter(HandSpec::hand),
                PtaCodecs.STRING_OR_LIST.optionalFieldOf("match", List.of()).forGetter(HandSpec::match),
                ConsumeSpec.CODEC.optionalFieldOf("consume", ConsumeSpec.NONE).forGetter(HandSpec::consume),
                NbtPairSpec.CODEC.optionalFieldOf("nbt", NbtPairSpec.EMPTY).forGetter(HandSpec::nbt),
                NbtPredicateSpec.CODEC.listOf().optionalFieldOf("nbt_predicates", List.of()).forGetter(HandSpec::nbtPredicates)
        ).apply(instance, HandSpec::new));
    }

    public record TargetSpec(
            String kind,
            List<String> match,
            StatePairSpec state,
            NbtPairSpec nbt,
            List<NbtPredicateSpec> nbtPredicates
    ) {
        public static final Codec<TargetSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.optionalFieldOf("kind", "block").forGetter(TargetSpec::kind),
                PtaCodecs.STRING_OR_LIST.optionalFieldOf("match", List.of()).forGetter(TargetSpec::match),
                StatePairSpec.CODEC.optionalFieldOf("state", StatePairSpec.EMPTY).forGetter(TargetSpec::state),
                NbtPairSpec.CODEC.optionalFieldOf("nbt", NbtPairSpec.EMPTY).forGetter(TargetSpec::nbt),
                NbtPredicateSpec.CODEC.listOf().optionalFieldOf("nbt_predicates", List.of()).forGetter(TargetSpec::nbtPredicates)
        ).apply(instance, TargetSpec::new));
    }

    // The block/fluid a transformation turns the target into.
    public record IntoSpec(String kind, String id, Map<String, String> state) {
        private static final Codec<Map<String, String>> STATE_MAP = Codec.unboundedMap(Codec.STRING, PtaCodecs.SCALAR_STRING);

        public static final Codec<IntoSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.optionalFieldOf("kind", "block").forGetter(IntoSpec::kind),
                Codec.STRING.fieldOf("id").forGetter(IntoSpec::id),
                STATE_MAP.optionalFieldOf("state", Map.of()).forGetter(IntoSpec::state)
        ).apply(instance, IntoSpec::new));
    }

    /**
     * Where a transformation lands, relative to the block that was interacted with.
     *
     * <p>{@code relative_to} picks the frame the three numbers are read in: {@code world} for the
     * plain world axes, {@code player} for the player's horizontal facing, {@code face} for the
     * clicked face. See {@code PtaOffset} for the exact axis roles.</p>
     */
    public record OffsetSpec(int x, int y, int z, String relativeTo) {
        public static final Codec<OffsetSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("x", 0).forGetter(OffsetSpec::x),
                Codec.INT.optionalFieldOf("y", 0).forGetter(OffsetSpec::y),
                Codec.INT.optionalFieldOf("z", 0).forGetter(OffsetSpec::z),
                Codec.STRING.optionalFieldOf("relative_to", "world").forGetter(OffsetSpec::relativeTo)
        ).apply(instance, OffsetSpec::new));

        public static final OffsetSpec NONE = new OffsetSpec(0, 0, 0, "world");
    }

    public record TransformationSpec(
            double chance,
            String op,
            Optional<OffsetSpec> at,
            Optional<TargetSpec> require,
            boolean drops,
            Optional<IntoSpec> into,
            Optional<CompoundTag> nbt,
            Optional<String> sound,
            Optional<String> particles
    ) {
        public static final Codec<TransformationSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.fieldOf("chance").forGetter(TransformationSpec::chance),
                // `replace` is what every transformation did before there were ops, so it stays the
                // default and nothing already written changes meaning.
                Codec.STRING.optionalFieldOf("op", "replace").forGetter(TransformationSpec::op),
                OffsetSpec.CODEC.optionalFieldOf("at").forGetter(TransformationSpec::at),
                // Same shape as `target`, but asked of the destination rather than of the block that
                // was clicked. Absent means the destination is not inspected at all.
                TargetSpec.CODEC.optionalFieldOf("require").forGetter(TransformationSpec::require),
                // `op: break` only. Whether the destroyed block yields its loot.
                Codec.BOOL.optionalFieldOf("drops", true).forGetter(TransformationSpec::drops),
                IntoSpec.CODEC.optionalFieldOf("into").forGetter(TransformationSpec::into),
                PtaCodecs.SNBT.optionalFieldOf("nbt").forGetter(TransformationSpec::nbt),
                Codec.STRING.optionalFieldOf("sound").forGetter(TransformationSpec::sound),
                Codec.STRING.optionalFieldOf("particles").forGetter(TransformationSpec::particles)
        ).apply(instance, TransformationSpec::new));
    }

    // A single weighted drop entry (the v1 "pool" element, with "weight" replacing "chance").
    public record RewardEntrySpec(List<String> match, int weight, CountSpec count, Optional<CompoundTag> nbt) {
        public static final Codec<RewardEntrySpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                PtaCodecs.STRING_OR_LIST.fieldOf("match").forGetter(RewardEntrySpec::match),
                Codec.INT.optionalFieldOf("weight", 1).forGetter(RewardEntrySpec::weight),
                CountSpec.CODEC.optionalFieldOf("count", CountSpec.exact(1)).forGetter(RewardEntrySpec::count),
                PtaCodecs.SNBT.optionalFieldOf("nbt").forGetter(RewardEntrySpec::nbt)
        ).apply(instance, RewardEntrySpec::new));
    }

    public record RewardsSpec(
            List<RewardEntrySpec> weighted,
            List<RewardEntrySpec> guaranteed,
            int rolls,
            Optional<FortuneSpec> fortune
    ) {
        public static final Codec<RewardsSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                RewardEntrySpec.CODEC.listOf().optionalFieldOf("weighted", List.of()).forGetter(RewardsSpec::weighted),
                RewardEntrySpec.CODEC.listOf().optionalFieldOf("guaranteed", List.of()).forGetter(RewardsSpec::guaranteed),
                Codec.INT.optionalFieldOf("rolls", 1).forGetter(RewardsSpec::rolls),
                FortuneSpec.CODEC.optionalFieldOf("fortune").forGetter(RewardsSpec::fortune)
        ).apply(instance, RewardsSpec::new));
    }

    // A single player cost (damage or hunger): chance to apply, and how much.
    public record CostSpec(double chance, CountSpec amount) {
        public static final Codec<CostSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.optionalFieldOf("chance", 1.0D).forGetter(CostSpec::chance),
                CountSpec.CODEC.fieldOf("amount").forGetter(CostSpec::amount)
        ).apply(instance, CostSpec::new));
    }

    public record CostsSpec(Optional<CostSpec> damage, Optional<CostSpec> hunger) {
        public static final Codec<CostsSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                CostSpec.CODEC.optionalFieldOf("damage").forGetter(CostsSpec::damage),
                CostSpec.CODEC.optionalFieldOf("hunger").forGetter(CostsSpec::hunger)
        ).apply(instance, CostsSpec::new));
    }

    public record BiomeSpec(List<String> whitelist, List<String> blacklist) {
        public static final Codec<BiomeSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.listOf().optionalFieldOf("whitelist", List.of()).forGetter(BiomeSpec::whitelist),
                Codec.STRING.listOf().optionalFieldOf("blacklist", List.of()).forGetter(BiomeSpec::blacklist)
        ).apply(instance, BiomeSpec::new));

        public static final BiomeSpec EMPTY = new BiomeSpec(List.of(), List.of());
    }

    public record LightSpec(Optional<Integer> min, Optional<Integer> max) {
        public static final Codec<LightSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("min").forGetter(LightSpec::min),
                Codec.INT.optionalFieldOf("max").forGetter(LightSpec::max)
        ).apply(instance, LightSpec::new));

        public static final LightSpec EMPTY = new LightSpec(Optional.empty(), Optional.empty());
    }

    public record PlayerStateSpec(int minFood, int minXpLevels) {
        public static final Codec<PlayerStateSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("min_food", 0).forGetter(PlayerStateSpec::minFood),
                Codec.INT.optionalFieldOf("min_xp_levels", 0).forGetter(PlayerStateSpec::minXpLevels)
        ).apply(instance, PlayerStateSpec::new));

        public static final PlayerStateSpec EMPTY = new PlayerStateSpec(0, 0);
    }

    public record ConditionsSpec(
            BiomeSpec biomes,
            String time,
            List<String> weather,
            Optional<List<Integer>> yRange,
            LightSpec light,
            Optional<Boolean> requiresSneaking,
            PlayerStateSpec playerState
    ) {
        public static final Codec<ConditionsSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BiomeSpec.CODEC.optionalFieldOf("biomes", BiomeSpec.EMPTY).forGetter(ConditionsSpec::biomes),
                Codec.STRING.optionalFieldOf("time", "any").forGetter(ConditionsSpec::time),
                Codec.STRING.listOf().optionalFieldOf("weather", List.of()).forGetter(ConditionsSpec::weather),
                Codec.INT.listOf().optionalFieldOf("y_range").forGetter(ConditionsSpec::yRange),
                LightSpec.CODEC.optionalFieldOf("light", LightSpec.EMPTY).forGetter(ConditionsSpec::light),
                Codec.BOOL.optionalFieldOf("requires_sneaking").forGetter(ConditionsSpec::requiresSneaking),
                PlayerStateSpec.CODEC.optionalFieldOf("player_state", PlayerStateSpec.EMPTY).forGetter(ConditionsSpec::playerState)
        ).apply(instance, ConditionsSpec::new));
    }

    public record EffectSpec(String id, int duration, int amplifier, double chance) {
        public static final Codec<EffectSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(EffectSpec::id),
                Codec.INT.optionalFieldOf("duration", 200).forGetter(EffectSpec::duration),
                Codec.INT.optionalFieldOf("amplifier", 0).forGetter(EffectSpec::amplifier),
                Codec.DOUBLE.optionalFieldOf("chance", 1.0D).forGetter(EffectSpec::chance)
        ).apply(instance, EffectSpec::new));
    }

    public record FortuneSpec(String enchant, double factor) {
        public static final Codec<FortuneSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.optionalFieldOf("enchant", "minecraft:fortune").forGetter(FortuneSpec::enchant),
                Codec.DOUBLE.optionalFieldOf("factor", 1.0D).forGetter(FortuneSpec::factor)
        ).apply(instance, FortuneSpec::new));
    }
}
