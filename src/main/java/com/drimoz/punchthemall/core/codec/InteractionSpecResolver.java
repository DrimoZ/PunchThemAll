package com.drimoz.punchthemall.core.codec;

import com.drimoz.punchthemall.core.checker.BlockChecker;
import com.drimoz.punchthemall.core.checker.FluidChecker;
import com.drimoz.punchthemall.core.checker.ItemChecker;
import com.drimoz.punchthemall.core.codec.InteractionSpec.*;
import com.drimoz.punchthemall.core.model.classes.*;
import com.drimoz.punchthemall.core.model.enums.PtaHandEnum;
import com.drimoz.punchthemall.core.model.enums.PtaTypeEnum;
import com.drimoz.punchthemall.core.model.records.PtaDropRecord;
import com.drimoz.punchthemall.core.model.records.PtaInteractionRecord;
import com.drimoz.punchthemall.core.model.records.PtaStateRecord;
import com.drimoz.punchthemall.core.util.PTALoggers;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

import static com.drimoz.punchthemall.core.registry.RegistryConstants.INCORRECT_FORMAT;
import static com.drimoz.punchthemall.core.registry.RegistryConstants.SAME_STATE;

/**
 * Turns a validated {@link InteractionSpec} (schema_version 2) into the existing runtime
 * {@link PtaInteraction} model. Structural problems are already caught by the codec; this stage
 * only resolves ids/tags against the live registries and logs unknown entries, exactly like the
 * legacy v1 creator, so gameplay behaviour is identical between the two formats.
 */
public final class InteractionSpecResolver {

    private static final char TAG_PREFIX = '#';

    private InteractionSpecResolver() {}

    public static PtaInteraction resolve(ResourceLocation id, InteractionSpec spec) {
        PtaTypeEnum type;
        try {
            type = PtaTypeEnum.fromString(spec.type());
        } catch (IllegalArgumentException e) {
            error(id, "Unknown interaction type " + spec.type());
            return null;
        }

        PtaHand hand = resolveHand(id, spec.hand().orElse(null));
        PtaBlock block = resolveTarget(id, spec.target().orElse(null));
        PtaTransformation transformation = resolveTransformation(id, spec.transformation().orElse(null));
        PtaRewards rewards = resolveRewards(id, spec.rewards().orElse(null));

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
        }

        PtaExtras extras = resolveExtras(id, spec);

        return new PtaInteraction(id, type, damage, hunger, hand, block, transformation, rewards, biomeWhitelist, biomeBlacklist, extras);
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

        return PtaHand.create(handEnum, itemSet, whitelist, blacklist, predicates, chance, damageable, consumable);
    }

    // Target (block / fluid / air)

    private static PtaBlock resolveTarget(ResourceLocation id, TargetSpec spec) {
        if (spec == null || spec.kind().equalsIgnoreCase("air")) {
            return PtaBlock.createAir();
        }

        String kind = spec.kind().toLowerCase(Locale.ROOT);
        Set<Block> blockSet = new HashSet<>();
        Set<Fluid> fluidSet = new HashSet<>();

        for (String entry : spec.match()) {
            boolean isTag = !entry.isEmpty() && entry.charAt(0) == TAG_PREFIX;
            String name = isTag ? entry.substring(1) : entry;
            switch (kind) {
                case "block" -> addBlock(id, blockSet, name, isTag, "target.match");
                case "fluid" -> addFluid(id, fluidSet, name, isTag, "target.match");
                case "any" -> {
                    boolean found = addBlock(id, blockSet, name, isTag, "target.match");
                    found |= addFluid(id, fluidSet, name, isTag, "target.match");
                    if (!found) error(id, "target.match - Unknown block/fluid " + entry);
                }
                default -> error(id, "target.kind - Unknown kind " + spec.kind());
            }
        }

        if (blockSet.isEmpty() && fluidSet.isEmpty()) {
            error(id, "target.match resolved to nothing; treating target as air");
            return PtaBlock.createAir();
        }

        if (!blockSet.isEmpty() && !fluidSet.isEmpty()) {
            error(id, "target cannot mix blocks and fluids; block targets will be used");
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

    private static PtaTransformation resolveTransformation(ResourceLocation id, TransformationSpec spec) {
        if (spec == null) return PtaTransformation.createAir(0, null, null);

        double chance = spec.chance();
        if (chance <= 0) return PtaTransformation.createAir(0, null, null);

        SoundEvent sound = spec.sound()
                .map(name -> ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(name)))
                .orElse(null);
        ParticleOptions particle = spec.particles()
                .filter(BlockChecker::doesBlockExist)
                .map(name -> (ParticleOptions) new BlockParticleOption(ParticleTypes.BLOCK, BlockChecker.getExistingBlock(name).defaultBlockState()))
                .orElse(null);

        IntoSpec into = spec.into().orElse(null);
        if (into == null) {
            return PtaTransformation.createAir(chance, sound, particle);
        }

        CompoundTag nbt = spec.nbt().orElse(new CompoundTag());
        String kind = into.kind().toLowerCase(Locale.ROOT);

        if (kind.equals("block")) {
            if (!BlockChecker.doesBlockExist(into.id())) {
                error(id, "transformation.into.id - Unknown block " + into.id());
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
                error(id, "transformation.into.id - Unknown fluid " + into.id());
                return PtaTransformation.createAir(chance, sound, particle);
            }
            Fluid fluid = FluidChecker.getExistingFluid(into.id());
            Set<PtaStateRecord<?>> state = buildStates(id, into.state(), Set.of(fluid));
            return PtaTransformation.createFluid(chance, fluid, state, nbt, sound, particle);
        }

        if (kind.equals("air")) {
            return PtaTransformation.createAir(chance, sound, particle);
        }

        error(id, "transformation.into.kind - Unknown kind " + into.kind());
        return PtaTransformation.createAir(chance, sound, particle);
    }

    // Rewards / pool

    private static PtaRewards resolveRewards(ResourceLocation id, RewardsSpec spec) {
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

            PtaDropRecord record = toDropRecord(id, entry, path);
            pool.put(record, entry.weight());
        }

        List<PtaDropRecord> guaranteed = new ArrayList<>();
        index = 0;
        for (RewardEntrySpec entry : spec.guaranteed()) {
            String path = "rewards.guaranteed[" + index + "]";
            index++;
            guaranteed.add(toDropRecord(id, entry, path));
        }

        Enchantment fortuneEnchant = null;
        double fortuneFactor = 0;
        if (spec.fortune().isPresent()) {
            String enchantId = spec.fortune().get().enchant();
            fortuneEnchant = ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation(enchantId));
            if (fortuneEnchant == null) {
                error(id, "rewards.fortune.enchant - Unknown enchantment " + enchantId);
            } else {
                fortuneFactor = spec.fortune().get().factor();
            }
        }

        return PtaRewards.create(PtaPool.create(pool), guaranteed, spec.rolls(), fortuneEnchant, fortuneFactor);
    }

    private static PtaDropRecord toDropRecord(ResourceLocation id, RewardEntrySpec entry, String path) {
        Set<Item> items = resolveItems(id, entry.match(), path + ".match");
        CountSpec.Range range = entry.count().resolve(0);
        CompoundTag nbt = items.isEmpty() ? null : entry.nbt().orElse(null);
        return new PtaDropRecord(items, range.min(), range.max(), nbt);
    }

    // Extras: conditions (non-biome) + player effects + interaction sound/particles

    private static PtaExtras resolveExtras(ResourceLocation id, InteractionSpec spec) {
        PtaConditions conditions = spec.conditions().map(InteractionSpecResolver::resolveConditions).orElse(PtaConditions.EMPTY);

        List<PtaEffect> effects = new ArrayList<>();
        for (EffectSpec effectSpec : spec.effects()) {
            MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(effectSpec.id()));
            if (effect == null) {
                error(id, "effects - Unknown effect " + effectSpec.id());
                continue;
            }
            effects.add(new PtaEffect(effect, effectSpec.duration(), effectSpec.amplifier(), effectSpec.chance()));
        }

        SoundEvent sound = spec.sound()
                .map(name -> ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(name)))
                .orElse(null);
        ParticleOptions particles = spec.particles()
                .filter(BlockChecker::doesBlockExist)
                .map(name -> (ParticleOptions) new BlockParticleOption(ParticleTypes.BLOCK, BlockChecker.getExistingBlock(name).defaultBlockState()))
                .orElse(null);

        if (conditions.isEmpty() && effects.isEmpty() && sound == null && particles == null) {
            return PtaExtras.EMPTY;
        }
        return new PtaExtras(conditions, effects, sound, particles);
    }

    private static PtaConditions resolveConditions(ConditionsSpec spec) {
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

        return new PtaConditions(time, weather, yMin, yMax, lightMin, lightMax, requiresSneaking,
                spec.playerState().minFood(), spec.playerState().minXpLevels());
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

    private static boolean addBlock(ResourceLocation id, Set<Block> blocks, String name, boolean isTag, String path) {
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
            error(id, path + " - Unknown block " + name);
            return false;
        } catch (RuntimeException e) {
            error(id, path + " - Invalid block entry " + name + " : " + e);
            return false;
        }
    }

    private static boolean addFluid(ResourceLocation id, Set<Fluid> fluids, String name, boolean isTag, String path) {
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
            error(id, path + " - Unknown fluid " + name);
            return false;
        } catch (RuntimeException e) {
            error(id, path + " - Invalid fluid entry " + name + " : " + e);
            return false;
        }
    }

    // State resolution (mirrors the legacy creator so behaviour matches v1 exactly)

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
