package com.drimoz.punchthemall.core.codec;

import com.drimoz.punchthemall.core.checker.BlockChecker;
import com.drimoz.punchthemall.core.checker.FluidChecker;
import com.drimoz.punchthemall.core.checker.ItemChecker;
import com.drimoz.punchthemall.core.codec.InteractionSpec.*;
import com.drimoz.punchthemall.core.model.classes.*;
import com.drimoz.punchthemall.core.model.enums.PtaHandEnum;
import com.drimoz.punchthemall.core.model.enums.PtaDropMode;
import com.drimoz.punchthemall.core.model.enums.PtaTransformOp;
import com.drimoz.punchthemall.core.model.enums.PtaTypeEnum;
import com.drimoz.punchthemall.core.model.records.PtaDropRecord;
import com.drimoz.punchthemall.core.model.records.PtaInteractionRecord;
import com.drimoz.punchthemall.core.model.records.PtaNeighbour;
import com.drimoz.punchthemall.core.model.records.PtaOffset;
import com.drimoz.punchthemall.core.model.records.PtaStateRecord;
import com.drimoz.punchthemall.core.util.PTALoggers;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

import java.util.*;

import static com.drimoz.punchthemall.core.registry.RegistryConstants.INCORRECT_FORMAT;
import static com.drimoz.punchthemall.core.registry.RegistryConstants.SAME_STATE;

/**
 * Turns a validated {@link InteractionSpec} (schema_version 2) into the runtime {@link PtaInteraction}
 * model. Resolves ids/tags against the live registries; enchantments come from the dynamic registry
 * via the supplied {@link HolderLookup.Provider}. Structural problems are already caught by the codec.
 */
public final class InteractionSpecResolver {

    private static final char TAG_PREFIX = '#';

    private InteractionSpecResolver() {}

    public static PtaInteraction resolve(ResourceLocation id, InteractionSpec spec, HolderLookup.Provider registries) {
        PtaTypeEnum type;
        try {
            type = PtaTypeEnum.fromString(spec.type());
        } catch (IllegalArgumentException e) {
            error(id, "Unknown interaction type " + spec.type());
            return null;
        }

        PtaHand hand = resolveHand(id, spec.hand().orElse(null));
        PtaBlock block = resolveTarget(id, spec.target().orElse(null));
        List<PtaTransformation> transformations = resolveTransformations(id, spec.transformation().all());
        double transformationChance = spec.transformation().chance();
        PtaRewards rewards = resolveRewards(id, spec.rewards().orElse(null), registries);

        PtaInteractionRecord damage = null;
        PtaInteractionRecord hunger = null;
        if (spec.costs().isPresent()) {
            CostsSpec costs = spec.costs().get();
            damage = costs.damage().map(c -> toRecord(c, 1)).orElse(null);
            hunger = costs.hunger().map(c -> toRecord(c, 1)).orElse(null);
        }

        Set<String> biomeWhitelist = new HashSet<>();
        Set<String> biomeBlacklist = new HashSet<>();
        if (spec.conditions().isPresent()) {
            BiomeSpec biomes = spec.conditions().get().biomes();
            biomeWhitelist.addAll(biomes.whitelist());
            biomeBlacklist.addAll(biomes.blacklist());
            if (!biomeWhitelist.isEmpty() && !biomeBlacklist.isEmpty()) {
                error(id, "conditions.biomes cannot define both whitelist and blacklist; whitelist will take precedence at runtime");
            }
            validateBiomeEntries(id, biomeWhitelist, "conditions.biomes.whitelist");
            validateBiomeEntries(id, biomeBlacklist, "conditions.biomes.blacklist");
        }

        PtaExtras extras = resolveExtras(id, spec);

        // Sneaking belongs to the type. A file that also sets conditions.requires_sneaking has said
        // it twice, and when the two disagree the interaction can never fire — the type filters for
        // one and the condition for the other. Rather than load something that cannot work, take the
        // condition as the intent, fold it into the type, and leave one source of truth behind. The
        // recipe viewers read the type, so this is also what stops them advertising a dead recipe.
        Boolean requiresSneaking = extras.conditions().requiresSneaking();
        if (requiresSneaking != null) {
            if (requiresSneaking != type.isShiftClick()) {
                PtaTypeEnum folded = type.withSneaking(requiresSneaking);
                PTALoggers.warn(id + " - conditions.requires_sneaking is " + requiresSneaking
                        + " but type is " + type.name().toLowerCase(Locale.ROOT)
                        + "; sneaking belongs to the type, so this is being read as "
                        + folded.name().toLowerCase(Locale.ROOT)
                        + ". Write that as the type and drop requires_sneaking.");
                type = folded;
            }
            extras = new PtaExtras(extras.conditions().withoutSneaking(), extras.effects(),
                    extras.sound(), extras.particles());
        }


        // The spec is a record of plain values, so its hashCode is a structural digest of the source
        // JSON — exactly what PtaInteraction.equals needs to tell "reloaded unchanged" from "edited".
        return new PtaInteraction(id, type, damage, hunger, hand, block, transformations, transformationChance, rewards,
                biomeWhitelist, biomeBlacklist, extras, spec.hidden(), spec.hashCode());
    }

    // Hand

    private static PtaHand resolveHand(ResourceLocation id, HandSpec spec) {
        if (spec == null) return PtaHand.createEmpty(PtaHandEnum.ANY_HAND);

        PtaHandEnum handEnum;
        try {
            handEnum = PtaHandEnum.fromValueOrName(spec.hand());
        } catch (IllegalArgumentException e) {
            error(id, "hand.hand - Unknown hand " + spec.hand());
            return PtaHand.createEmpty(PtaHandEnum.ANY_HAND);
        }

        if (spec.match().isEmpty()) {
            return PtaHand.createEmpty(handEnum);
        }

        Set<Item> itemSet = resolveItems(id, spec.match(), "hand.match");
        CompoundTag whitelist = spec.nbt().whitelist().orElse(new CompoundTag());
        CompoundTag blacklist = spec.nbt().blacklist().orElse(new CompoundTag());
        List<PtaNbtPredicate> predicates = toPredicates(spec.nbtPredicates());

        ConsumeSpec consume = spec.consume();
        boolean damageable = consume.mode().equalsIgnoreCase("durability");
        boolean consumable = consume.mode().equalsIgnoreCase("shrink") || consume.mode().equalsIgnoreCase("consume");
        double chance = consume.chance();
        CountSpec.Range count = consume.count().resolve(1);

        return PtaHand.create(handEnum, itemSet, whitelist, blacklist, predicates, chance, damageable, consumable, count.min(), count.max());
    }

    // Target (block / fluid / air)

    private static PtaBlock resolveTarget(ResourceLocation id, TargetSpec spec) {
        return resolveTarget(id, spec, "target");
    }

    /**
     * @param path where this selector sits in the file, so the log points at the field the author
     *             actually wrote — the same shape is used by {@code target} and by a transformation's
     *             {@code require}.
     */
    private static PtaBlock resolveTarget(ResourceLocation id, TargetSpec spec, String path) {
        if (spec == null || spec.kind().equalsIgnoreCase("air")) {
            return PtaBlock.createAir();
        }

        String kind = spec.kind().toLowerCase(Locale.ROOT);
        String matchPath = path + ".match";
        Set<Block> blockSet = new HashSet<>();
        Set<Fluid> fluidSet = new HashSet<>();

        for (String entry : spec.match()) {
            boolean isTag = !entry.isEmpty() && entry.charAt(0) == TAG_PREFIX;
            String name = isTag ? entry.substring(1) : entry;
            switch (kind) {
                case "block" -> addBlock(id, blockSet, name, isTag, matchPath, true);
                case "fluid" -> addFluid(id, fluidSet, name, isTag, matchPath, true);
                case "any" -> {
                    // Trying both sides is the point of "any", so neither lookup reports on its
                    // own — only failing at both is an error worth showing.
                    boolean found = addBlock(id, blockSet, name, isTag, matchPath, false);
                    found |= addFluid(id, fluidSet, name, isTag, matchPath, false);
                    if (!found) error(id, matchPath + " - Unknown block/fluid " + entry);
                }
                default -> error(id, path + ".kind - Unknown kind " + spec.kind());
            }
        }

        if (blockSet.isEmpty() && fluidSet.isEmpty()) {
            error(id, matchPath + " resolved to nothing; treating " + path + " as air");
            return PtaBlock.createAir();
        }

        if (!blockSet.isEmpty() && !fluidSet.isEmpty()) {
            error(id, path + " cannot mix blocks and fluids; block entries will be used");
            fluidSet.clear();
        }

        boolean isBlock = !blockSet.isEmpty();
        Set<?> entries = isBlock ? blockSet : fluidSet;

        Set<PtaStateRecord<?>> stateWhitelist = buildStates(id, spec.state().whitelist(), entries);
        Set<PtaStateRecord<?>> stateBlacklist = buildStates(id, spec.state().blacklist(), entries);

        CompoundTag nbtWhitelist = spec.nbt().whitelist().orElse(new CompoundTag());
        CompoundTag nbtBlacklist = spec.nbt().blacklist().orElse(new CompoundTag());
        List<PtaNbtPredicate> predicates = toPredicates(spec.nbtPredicates());

        return isBlock
                ? PtaBlock.createBlock(blockSet, stateWhitelist, stateBlacklist, nbtWhitelist, nbtBlacklist, predicates)
                : PtaBlock.createFluid(fluidSet, stateWhitelist, stateBlacklist, nbtWhitelist, nbtBlacklist, predicates);
    }

    // Transformation

    private static List<PtaTransformation> resolveTransformations(ResourceLocation id, List<TransformationSpec> specs) {
        if (specs.isEmpty()) return List.of();

        List<PtaTransformation> resolved = new ArrayList<>(specs.size());
        boolean single = specs.size() == 1;
        for (int index = 0; index < specs.size(); index++) {
            // A single transformation is still written as a bare object, so naming an index in the
            // log would point at something the author cannot find in their file.
            String path = single ? "transformation" : "transformation[" + index + "]";
            PtaTransformation transformation = resolveTransformation(id, specs.get(index), path);
            if (transformation != null) resolved.add(transformation);
        }
        return List.copyOf(resolved);
    }

    /** @return the transformation, or {@code null} when this entry can never do anything. */
    private static PtaTransformation resolveTransformation(ResourceLocation id, TransformationSpec spec, String path) {
        if (spec == null || spec.chance() <= 0) return null;

        double chance = spec.chance();

        PtaTransformOp op;
        try {
            op = PtaTransformOp.fromString(spec.op());
        } catch (IllegalArgumentException e) {
            error(id, path + ".op - Unknown op " + spec.op() + " (expected replace, break or place)");
            return null;
        }

        PtaOffset offset = resolveOffset(id, spec.at().orElse(null), path + ".at");
        PtaDropMode dropMode = resolveDropMode(id, spec.drops(), path + ".drops");
        PtaBlock require = resolveRequirement(id, spec.require().orElse(null), path + ".require");

        SoundEvent sound = resolveSound(id, spec.sound().orElse(null), path + ".sound");
        ParticleOptions particle = resolveParticles(id, spec.particles().orElse(null), path + ".particles");

        IntoSpec into = spec.into().orElse(null);

        if (op == PtaTransformOp.BREAK) {
            if (into != null) {
                error(id, path + ".into - op break destroys the block and writes nothing; into is ignored");
            }
            return PtaTransformation.createAir(chance, sound, particle)
                    .withPlacement(op, offset, require, dropMode);
        }

        if (op == PtaTransformOp.PLACE && into == null) {
            error(id, path + ".into - op place needs a block or fluid to place");
            return null;
        }

        PtaTransformation written = resolveInto(id, chance, into, spec.nbt().orElse(new CompoundTag()), sound, particle, path);
        return written.withPlacement(op, offset, require, dropMode);
    }

    private static PtaTransformation resolveInto(
            ResourceLocation id, double chance, IntoSpec into,
            CompoundTag nbt, SoundEvent sound, ParticleOptions particle, String path
    ) {
        if (into == null) {
            return PtaTransformation.createAir(chance, sound, particle);
        }

        String kind = into.kind().toLowerCase(Locale.ROOT);

        if (kind.equals("block")) {
            if (!BlockChecker.doesBlockExist(into.id())) {
                error(id, path + ".into.id - Unknown block " + into.id());
                return PtaTransformation.createAir(chance, sound, particle);
            }
            Block block = BlockChecker.getExistingBlock(into.id());
            if (block.equals(Blocks.AIR)) {
                return PtaTransformation.createAir(chance, sound, particle);
            }
            Set<PtaStateRecord<?>> state = buildStates(id, into.state(), Set.of(block));
            return PtaTransformation.createBlock(chance, block, state, nbt, sound, particle);
        }

        if (kind.equals("fluid")) {
            if (!FluidChecker.doesFluidExist(into.id())) {
                error(id, path + ".into.id - Unknown fluid " + into.id());
                return PtaTransformation.createAir(chance, sound, particle);
            }
            Fluid fluid = FluidChecker.getExistingFluid(into.id());
            Set<PtaStateRecord<?>> state = buildStates(id, into.state(), Set.of(fluid));
            return PtaTransformation.createFluid(chance, fluid, state, nbt, sound, particle);
        }

        if (kind.equals("copy")) {
            // Reads the block standing at `from` when the click happens, so pairing this with a
            // break at the same place moves a block rather than duplicating one.
            PtaOffset from = resolveOffset(id, into.from().orElse(null), path + ".into.from");
            return PtaTransformation.createCopy(chance, from, nbt, sound, particle);
        }

        if (kind.equals("air")) {
            return PtaTransformation.createAir(chance, sound, particle);
        }

        error(id, path + ".into.kind - Unknown kind " + into.kind());
        return PtaTransformation.createAir(chance, sound, particle);
    }

    private static PtaDropMode resolveDropMode(ResourceLocation id, String mode, String path) {
        try {
            return PtaDropMode.fromString(mode);
        } catch (IllegalArgumentException e) {
            error(id, path + " - Unknown drop mode " + mode + " (expected true, false, \"vanilla\", \"none\" or \"tool\")");
            return PtaDropMode.VANILLA;
        }
    }

    private static PtaOffset resolveOffset(ResourceLocation id, OffsetSpec spec, String path) {
        if (spec == null) return PtaOffset.NONE;

        PtaOffset.Frame resolvedFrame;
        try {
            resolvedFrame = PtaOffset.Frame.fromString(spec.relativeTo());
        } catch (IllegalArgumentException e) {
            error(id, path + ".relative_to - Unknown frame " + spec.relativeTo() + " (expected world, player or face); using world");
            resolvedFrame = PtaOffset.Frame.WORLD;
        }

        // The far corner, when the file gave one. Forgetting to read it here is what made every
        // region silently collapse to a single block: the JSON parsed, the maths was right, and
        // nothing between the two ever carried the second corner across.
        PtaOffset.Frame frame = resolvedFrame;
        PtaOffset to = spec.to()
                .map(corner -> new PtaOffset(corner.x(), corner.y(), corner.z(), frame))
                .orElse(null);

        return new PtaOffset(spec.x(), spec.y(), spec.z(), frame, to);
    }

    /**
     * A {@code require} that resolves to nothing would silently match no destination and make the
     * whole transformation dead, which reads as a mod bug. Report it and drop the requirement
     * instead — the op's own rules (a {@code place} still needs room) remain in force.
     */
    private static PtaBlock resolveRequirement(ResourceLocation id, TargetSpec spec, String path) {
        if (spec == null) return null;

        PtaBlock require = resolveTarget(id, spec, path);
        if (require.isAir()) {
            error(id, path + " - names no block or fluid; requirement ignored"
                    + " (write match: [\"minecraft:air\"] to require an empty destination)");
            return null;
        }
        return require;
    }

    // Rewards / pool

    private static PtaRewards resolveRewards(ResourceLocation id, RewardsSpec spec, HolderLookup.Provider registries) {
        if (spec == null) return PtaRewards.of(PtaPool.create(new HashMap<>()));

        Map<PtaDropRecord, Integer> pool = new HashMap<>();
        int index = 0;
        for (RewardEntrySpec entry : spec.weighted()) {
            String path = "rewards.weighted[" + index + "]";
            index++;

            if (entry.weight() <= 0) {
                error(id, path + ".weight must be greater than 0");
                continue;
            }

            pool.put(toDropRecord(id, entry, path), entry.weight());
        }

        List<PtaDropRecord> guaranteed = new ArrayList<>();
        index = 0;
        for (RewardEntrySpec entry : spec.guaranteed()) {
            String path = "rewards.guaranteed[" + index + "]";
            index++;
            guaranteed.add(toDropRecord(id, entry, path));
        }

        Holder<Enchantment> fortuneEnchant = null;
        double fortuneFactor = 0;
        if (spec.fortune().isPresent()) {
            String enchantId = spec.fortune().get().enchant();
            fortuneEnchant = resolveEnchantment(registries, enchantId);
            if (fortuneEnchant == null) {
                error(id, "rewards.fortune.enchant - Unknown enchantment " + enchantId);
            } else {
                fortuneFactor = spec.fortune().get().factor();
            }
        }

        PtaOffset dropAt = resolveOffset(id, spec.at().orElse(null), "rewards.at");
        return PtaRewards.create(PtaPool.create(pool), guaranteed, spec.rolls(), fortuneEnchant, fortuneFactor)
                .droppingAt(dropAt);
    }

    private static PtaDropRecord toDropRecord(ResourceLocation id, RewardEntrySpec entry, String path) {
        Set<Item> items = resolveItems(id, entry.match(), path + ".match");
        CountSpec.Range range = entry.count().resolve(0);
        CompoundTag nbt = items.isEmpty() ? null : entry.nbt().orElse(null);
        return new PtaDropRecord(items, range.min(), range.max(), nbt);
    }

    // Extras: conditions (non-biome) + player effects + interaction sound/particles

    private static PtaExtras resolveExtras(ResourceLocation id, InteractionSpec spec) {
        PtaConditions conditions = spec.conditions().map(conditionsSpec -> resolveConditions(id, conditionsSpec)).orElse(PtaConditions.EMPTY);

        List<PtaEffect> effects = new ArrayList<>();
        for (EffectSpec effectSpec : spec.effects()) {
            ResourceLocation effectId = tryParse(effectSpec.id());
            Holder<MobEffect> effect = effectId == null ? null : BuiltInRegistries.MOB_EFFECT
                    .getHolder(ResourceKey.create(Registries.MOB_EFFECT, effectId))
                    .orElse(null);
            if (effect == null) {
                error(id, "effects - Unknown effect " + effectSpec.id());
                continue;
            }
            effects.add(new PtaEffect(effect, effectSpec.duration(), effectSpec.amplifier(), effectSpec.chance()));
        }

        SoundEvent sound = resolveSound(id, spec.sound().orElse(null), "sound");
        ParticleOptions particles = resolveParticles(id, spec.particles().orElse(null), "particles");

        if (conditions.isEmpty() && effects.isEmpty() && sound == null && particles == null) {
            return PtaExtras.EMPTY;
        }
        return new PtaExtras(conditions, effects, sound, particles);
    }

    private static PtaConditions resolveConditions(ResourceLocation id, ConditionsSpec spec) {
        PtaConditions.Time time = switch (spec.time().toLowerCase(Locale.ROOT)) {
            case "day" -> PtaConditions.Time.DAY;
            case "night" -> PtaConditions.Time.NIGHT;
            default -> PtaConditions.Time.ANY;
        };

        Set<PtaConditions.Weather> weather = new HashSet<>();
        for (String token : spec.weather()) {
            switch (token.toLowerCase(Locale.ROOT)) {
                case "clear" -> weather.add(PtaConditions.Weather.CLEAR);
                case "rain" -> weather.add(PtaConditions.Weather.RAIN);
                case "thunder" -> weather.add(PtaConditions.Weather.THUNDER);
                default -> { /* ignore unknown weather token */ }
            }
        }

        Integer yMin = null;
        Integer yMax = null;
        if (spec.yRange().isPresent()) {
            List<Integer> range = spec.yRange().get();
            if (!range.isEmpty()) yMin = range.get(0);
            if (range.size() > 1) yMax = range.get(1);
        }

        Integer lightMin = spec.light().min().orElse(null);
        Integer lightMax = spec.light().max().orElse(null);
        Boolean requiresSneaking = spec.requiresSneaking().orElse(null);

        List<PtaNeighbour> neighbours = new ArrayList<>();
        int index = 0;
        for (NeighbourSpec neighbourSpec : spec.neighbours()) {
            String path = "conditions.neighbours[" + index + "]";
            index++;

            PtaBlock block = resolveTarget(id, neighbourSpec.block(), path + ".block");
            if (block.isAir()) {
                error(id, path + ".block - names no block or fluid; this condition can never hold and was dropped");
                continue;
            }
            neighbours.add(new PtaNeighbour(
                    resolveOffset(id, neighbourSpec.at(), path + ".at"),
                    block, neighbourSpec.invert()));
        }

        return new PtaConditions(time, weather, yMin, yMax, lightMin, lightMax, requiresSneaking,
                spec.playerState().minFood(), spec.playerState().minXpLevels(), List.copyOf(neighbours));
    }

    // Registry resolution helpers

    // Feedback that cannot be resolved is dropped, but never silently: an interaction that plays no
    // sound looks identical to one that was authored without a sound, so without a line in the log
    // there is nothing for the author to go on.

    private static SoundEvent resolveSound(ResourceLocation id, String name, String path) {
        if (name == null) return null;

        ResourceLocation soundId = tryParse(name);
        SoundEvent sound = soundId == null ? null : BuiltInRegistries.SOUND_EVENT.get(soundId);
        if (sound == null) {
            error(id, path + " - Unknown sound " + name);
        }
        return sound;
    }

    private static ParticleOptions resolveParticles(ResourceLocation id, String name, String path) {
        if (name == null) return null;

        if (!BlockChecker.doesBlockExist(name)) {
            // `particles` takes a block id (block-break particles), not a particle-type id — the
            // single most common mistake here, so say which kind of id is expected.
            error(id, path + " - Unknown block " + name + " (particles takes a block id, not a particle id)");
            return null;
        }
        return new BlockParticleOption(ParticleTypes.BLOCK, BlockChecker.getExistingBlock(name).defaultBlockState());
    }

    /**
     * Report biome/dimension entries that can never match. They are matched as plain strings at click
     * time, so an unparseable tag or a typo is otherwise invisible — the interaction simply never
     * fires, which reads as a mod bug rather than a file one.
     */
    private static void validateBiomeEntries(ResourceLocation id, Set<String> entries, String path) {
        for (String entry : entries) {
            if (entry.isEmpty()) {
                error(id, path + " - empty biome entry");
            } else if (entry.charAt(0) == '#' && tryParse(entry.substring(1)) == null) {
                error(id, path + " - Malformed biome tag " + entry);
            } else if (entry.charAt(0) != '#' && tryParse(entry) == null) {
                error(id, path + " - Malformed biome/dimension id " + entry);
            }
        }
    }

    private static Holder<Enchantment> resolveEnchantment(HolderLookup.Provider registries, String enchantId) {
        ResourceLocation parsed = tryParse(enchantId);
        if (registries == null || parsed == null) return null;
        return registries.lookupOrThrow(Registries.ENCHANTMENT)
                .get(ResourceKey.create(Registries.ENCHANTMENT, parsed))
                .map(holder -> (Holder<Enchantment>) holder)
                .orElse(null);
    }

    /**
     * Every id in an interaction file is authored text, so a malformed one is expected input.
     * {@code ResourceLocation.parse} throws, and these resolutions run inside the datapack reload —
     * one typo would abort the load of every interaction rather than reporting that one file.
     */
    private static ResourceLocation tryParse(String id) {
        return id == null ? null : ResourceLocation.tryParse(id);
    }

    // Costs

    private static PtaInteractionRecord toRecord(CostSpec spec, int defaultMin) {
        CountSpec.Range range = spec.amount().resolve(defaultMin);
        return new PtaInteractionRecord(spec.chance(), range.min(), range.max());
    }

    // NBT predicates

    private static List<PtaNbtPredicate> toPredicates(List<NbtPredicateSpec> specs) {
        if (specs.isEmpty()) return List.of();
        List<PtaNbtPredicate> result = new ArrayList<>(specs.size());
        for (NbtPredicateSpec spec : specs) {
            Optional<Integer> min = Optional.empty();
            Optional<Integer> max = Optional.empty();
            if (spec.intRange().isPresent()) {
                List<Integer> range = spec.intRange().get();
                if (!range.isEmpty()) min = Optional.of(range.get(0));
                if (range.size() > 1) max = Optional.of(range.get(1));
            }
            result.add(new PtaNbtPredicate(spec.path(), min, max, spec.where().orElse(new CompoundTag())));
        }
        return result;
    }

    // Selector resolution

    private static Set<Item> resolveItems(ResourceLocation id, List<String> entries, String path) {
        Set<Item> items = new HashSet<>();
        for (String entry : entries) {
            boolean isTag = !entry.isEmpty() && entry.charAt(0) == TAG_PREFIX;
            String name = isTag ? entry.substring(1) : entry;
            try {
                if (isTag) {
                    Set<Item> tagItems = ItemChecker.getItemsForTag(name);
                    if (tagItems.isEmpty()) {
                        error(id, path + " - Unknown or empty item tag " + name);
                        continue;
                    }
                    items.addAll(tagItems);
                } else if (ItemChecker.doesItemExist(name)) {
                    items.add(ItemChecker.getExistingItem(name));
                } else {
                    error(id, path + " - Unknown item " + name);
                }
            } catch (RuntimeException e) {
                error(id, path + " - Invalid item entry " + entry + " : " + e);
            }
        }
        return items;
    }

    private static boolean addBlock(ResourceLocation id, Set<Block> blocks, String name, boolean isTag, String path, boolean reportMissing) {
        try {
            if (isTag) {
                if (!BlockChecker.isBlockTagExisting(name)) return false;
                Set<Block> tagBlocks = BlockChecker.getBlocksForTag(name);
                blocks.addAll(tagBlocks);
                return !tagBlocks.isEmpty();
            }
            if (BlockChecker.doesBlockExist(name)) {
                blocks.add(BlockChecker.getExistingBlock(name));
                return true;
            }
            if (reportMissing) error(id, path + " - Unknown block " + name);
            return false;
        } catch (RuntimeException e) {
            error(id, path + " - Invalid block entry " + name + " : " + e);
            return false;
        }
    }

    private static boolean addFluid(ResourceLocation id, Set<Fluid> fluids, String name, boolean isTag, String path, boolean reportMissing) {
        try {
            if (isTag) {
                if (!FluidChecker.isFluidTagExisting(name)) return false;
                Set<Fluid> tagFluids = FluidChecker.getFluidsForTag(name);
                fluids.addAll(tagFluids);
                return !tagFluids.isEmpty();
            }
            if (FluidChecker.doesFluidExist(name)) {
                fluids.add(FluidChecker.getExistingFluid(name));
                return true;
            }
            if (reportMissing) error(id, path + " - Unknown fluid " + name);
            return false;
        } catch (RuntimeException e) {
            error(id, path + " - Invalid fluid entry " + name + " : " + e);
            return false;
        }
    }

    // State resolution

    private static Set<PtaStateRecord<?>> buildStates(ResourceLocation id, Map<String, String> states, Set<?> entries) {
        Set<PtaStateRecord<?>> result = new HashSet<>();
        for (Map.Entry<String, String> stateEntry : states.entrySet()) {
            Property<?> property = null;
            for (Object entry : entries) {
                if (entry instanceof Fluid fluid) {
                    property = getPropertyByName(fluid.defaultFluidState(), stateEntry.getKey());
                } else if (entry instanceof Block block) {
                    property = getPropertyByName(block.defaultBlockState(), stateEntry.getKey());
                }
                if (property == null) break;
            }
            if (property != null) {
                addStateEntry(id, result, property, stateEntry.getValue());
            }
        }
        return result;
    }

    private static <T extends Comparable<T>> void addStateEntry(ResourceLocation id, Set<PtaStateRecord<?>> states, Property<T> property, String value) {
        T parsed = parsePropertyValue(property, value);
        if (value.equalsIgnoreCase(SAME_STATE) || parsed != null) {
            states.add(new PtaStateRecord<>(property, value));
        } else {
            error(id, "Failed to parse value " + value + " for property " + property.getName());
        }
    }

    private static <T extends Comparable<T>> T parsePropertyValue(Property<T> property, String value) {
        for (T possibleValue : property.getPossibleValues()) {
            if (possibleValue.toString().equalsIgnoreCase(value)) {
                return possibleValue;
            }
        }
        return null;
    }

    private static Property<?> getPropertyByName(BlockState state, String name) {
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equalsIgnoreCase(name)) return property;
        }
        return null;
    }

    private static Property<?> getPropertyByName(FluidState state, String name) {
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equalsIgnoreCase(name)) return property;
        }
        return null;
    }

    private static void error(ResourceLocation id, String message) {
        PTALoggers.error(INCORRECT_FORMAT + " - " + id.getPath() + " - " + message);
    }
}
