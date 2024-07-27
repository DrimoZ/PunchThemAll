package com.drimoz.punchthemall.core.registry;

import com.drimoz.punchthemall.core.checker.BlockChecker;
import com.drimoz.punchthemall.core.checker.FluidChecker;
import com.drimoz.punchthemall.core.checker.ItemChecker;
import com.drimoz.punchthemall.core.model.classes.*;
import com.drimoz.punchthemall.core.model.enums.PtaHandEnum;
import com.drimoz.punchthemall.core.model.enums.PtaTypeEnum;
import com.drimoz.punchthemall.core.model.records.PtaDropRecord;
import com.drimoz.punchthemall.core.model.records.PtaInteractionRecord;
import com.drimoz.punchthemall.core.model.records.PtaStateRecord;
import com.drimoz.punchthemall.core.util.PTALoggers;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

import static com.drimoz.punchthemall.core.registry.RegistryConstants.*;


public class InteractionCreator {

    public static PtaInteraction createInteraction(ResourceLocation id, JsonObject json) throws JsonSyntaxException {
        if (!json.has(STRING_TYPE)) {
            errorMissing(id, STRING_TYPE);
            return null;
        }

        PtaInteractionRecord hunger = null;
        if (json.has(STRING_HUNGER) && json.get(STRING_HUNGER).isJsonObject()) {
            JsonObject hungerJson = GsonHelper.getAsJsonObject(json, STRING_HUNGER);

            if (hungerJson.has(STRING_HUNGER_CHANCE) && hungerJson.get(STRING_HUNGER_CHANCE).isJsonPrimitive()) {
                double chance = GsonHelper.getAsDouble(hungerJson, STRING_HUNGER_CHANCE);

                if (hungerJson.has(STRING_HUNGER_COUNT) && hungerJson.get(STRING_HUNGER_COUNT).isJsonPrimitive()) {
                    int count = GsonHelper.getAsInt(hungerJson, STRING_HUNGER_COUNT);
                    hunger = new PtaInteractionRecord(chance, count, count);
                }
                else if(hungerJson.has(STRING_HUNGER_MIN) && hungerJson.get(STRING_HUNGER_MIN).isJsonPrimitive() &&
                        hungerJson.has(STRING_HUNGER_MAX) && hungerJson.get(STRING_HUNGER_MAX).isJsonPrimitive()) {
                    int min = GsonHelper.getAsInt(hungerJson, STRING_HUNGER_MIN);
                    int max = GsonHelper.getAsInt(hungerJson, STRING_HUNGER_MAX);
                    hunger = new PtaInteractionRecord(chance, min, max);
                }
            }
        }

        PtaInteractionRecord damage = null;
        if (json.has(STRING_DAMAGE) && json.get(STRING_DAMAGE).isJsonObject()) {
            JsonObject damageJson = GsonHelper.getAsJsonObject(json, STRING_DAMAGE);

            if (damageJson.has(STRING_DAMAGE_CHANCE) && damageJson.get(STRING_DAMAGE_CHANCE).isJsonPrimitive()) {
                double chance = GsonHelper.getAsDouble(damageJson, STRING_DAMAGE_CHANCE);

                if (damageJson.has(STRING_DAMAGE_COUNT) && damageJson.get(STRING_DAMAGE_COUNT).isJsonPrimitive()) {
                    int count = GsonHelper.getAsInt(damageJson, STRING_DAMAGE_COUNT);
                    damage = new PtaInteractionRecord(chance, count, count);
                }
                else if(damageJson.has(STRING_DAMAGE_MIN) && damageJson.get(STRING_DAMAGE_MIN).isJsonPrimitive() &&
                        damageJson.has(STRING_DAMAGE_MAX) && damageJson.get(STRING_DAMAGE_MAX).isJsonPrimitive()) {
                    int min = GsonHelper.getAsInt(damageJson, STRING_DAMAGE_MIN);
                    int max = GsonHelper.getAsInt(damageJson, STRING_DAMAGE_MAX);
                    damage = new PtaInteractionRecord(chance, min, max);
                }
            }
        }

        // Type
        PtaTypeEnum type = PtaTypeEnum.fromString(GsonHelper.getAsString(json, STRING_TYPE));

        // Hand
        PtaHand hand = createHand(id, json);

        // Block
        PtaBlock block = createBlock(id, json);

        // Transformation
        PtaTransformation transformation = createTransformation(id, json);

        // Pool
        PtaPool pool = createPool(id, json);

        // Biomes
        Set<String> biomeWhiteList = new HashSet<>(), biomeBlackList = new HashSet<>();
        if (json.has(STRING_BIOME) && json.get(STRING_BIOME).isJsonObject()) {
            JsonObject biomeJson = GsonHelper.getAsJsonObject(json, STRING_BIOME);

            if (biomeJson.has(STRING_BIOME_WHITELIST) && biomeJson.get(STRING_BIOME_WHITELIST).isJsonArray()) {
                biomeWhiteList.addAll(GsonHelper.getAsJsonArray(biomeJson, STRING_BIOME_WHITELIST).asList().stream().map(jsonElement -> jsonElement.getAsString().replace("\"", "")).toList());
            }
            else if (biomeJson.has(STRING_BIOME_BLACKLIST) && biomeJson.get(STRING_BIOME_BLACKLIST).isJsonArray()) {
                biomeBlackList.addAll(GsonHelper.getAsJsonArray(biomeJson, STRING_BIOME_BLACKLIST).asList().stream().map(jsonElement -> jsonElement.getAsString().replace("\"", "")).toList());
            }
        }

        return new PtaInteraction(id, type, damage, hunger, hand, block, transformation, pool, biomeWhiteList, biomeBlackList);
    }

    // Inner Work ( Global )

    private static void errorMissing(ResourceLocation id, String path) {
        errorFormat(id, "Missing " + path);
    }

    private static void errorFormat(ResourceLocation id, String error) {
        PTALoggers.error(INCORRECT_FORMAT + " - " + id.getPath() + " - " + error);
    }

    // Inner Work ( Hand )

    private static PtaHand createHand(ResourceLocation id, JsonObject json) {
        if (!json.has(STRING_HAND) || !json.get(STRING_HAND).isJsonObject()) return PtaHand.createEmpty(PtaHandEnum.ANY_HAND);

        // Json
        JsonObject handJson = GsonHelper.getAsJsonObject(json, STRING_HAND);

        // Hand ( Any - Main - Off )
        if (!handJson.has(STRING_HAND_HAND) || !handJson.get(STRING_HAND_HAND).isJsonPrimitive()) {
            errorMissing(id, STRING_HAND_HAND_FULL);
            return null;
        }
        PtaHandEnum hand = PtaHandEnum.fromValueOrName(GsonHelper.getAsString(handJson, STRING_HAND_HAND));

        // Items
        if (!handJson.has(STRING_HAND_ITEM)) {
            errorMissing(id, STRING_HAND_ITEM_FULL);
            return PtaHand.createEmpty(hand);
        }
        JsonObject handItemJson = GsonHelper.getAsJsonObject(handJson, STRING_HAND_ITEM);
        Set<Item> itemSet = createHandItemSet(id, handItemJson);

        Map.Entry<CompoundTag, CompoundTag> itemNBTs = Map.entry(new CompoundTag(), new CompoundTag());
        if (!itemSet.isEmpty() && handItemJson.has(STRING_HAND_ITEM_NBT) && handItemJson.get(STRING_HAND_ITEM_NBT).isJsonObject()) {
            itemNBTs = createHandItemNbt(id, GsonHelper.getAsJsonObject(handItemJson, STRING_HAND_ITEM_NBT));
        }

        boolean damageable = false;
        if (handJson.has(STRING_HAND_ITEM_DAMAGEABLE) && handJson.get(STRING_HAND_ITEM_DAMAGEABLE).isJsonPrimitive() && !itemSet.isEmpty()) {
            damageable = GsonHelper.getAsBoolean(handJson, STRING_HAND_ITEM_DAMAGEABLE);
        }

        boolean consumable = false;
        if (handJson.has(STRING_HAND_ITEM_CONSUMABLE) && handJson.get(STRING_HAND_ITEM_CONSUMABLE).isJsonPrimitive() && !itemSet.isEmpty()) {
            consumable = GsonHelper.getAsBoolean(handJson, STRING_HAND_ITEM_CONSUMABLE);
        }

        double chance = 1;
        if (handJson.has(STRING_HAND_ITEM_CHANCE) && handJson.get(STRING_HAND_ITEM_CHANCE).isJsonPrimitive() && !itemSet.isEmpty()) {
            chance = GsonHelper.getAsDouble(handJson, STRING_HAND_ITEM_CHANCE);
        }

        return PtaHand.create(hand, itemSet, itemNBTs.getKey(), itemNBTs.getValue(), chance, damageable, consumable);
    }

    private static Set<Item> createHandItemSet(ResourceLocation id, JsonObject json) {
        Set<Item> itemSet = new HashSet<>();

        if (json.has(STRING_HAND_ITEM_TAG)) {
            itemSet.addAll(ItemChecker.getItemsForTag(GsonHelper.getAsString(json, STRING_HAND_ITEM_TAG)));
        }
        else if (json.has(STRING_HAND_ITEM_ITEM)) {
            itemSet.add(ItemChecker.getExistingItem(GsonHelper.getAsString(json, STRING_HAND_ITEM_ITEM)));
        }
        else {
            errorMissing(id, STRING_HAND_ITEM_ITEM_FULL + " or " + STRING_HAND_ITEM_TAG_FULL);
        }

        return itemSet;
    }

    private static Map.Entry<CompoundTag, CompoundTag> createHandItemNbt(ResourceLocation id, JsonObject json) {
        CompoundTag nbtWhiteList = new CompoundTag(), nbtBlackList = new CompoundTag();

        if (json.has(STRING_HAND_ITEM_NBT_WHITELIST)) {
            nbtWhiteList = getNbtFromString(id, GsonHelper.getAsJsonObject(json, STRING_HAND_ITEM_NBT_WHITELIST));
        }
        if (json.has(STRING_HAND_ITEM_NBT_BLACKLIST)) {
            nbtBlackList = getNbtFromString(id, GsonHelper.getAsJsonObject(json, STRING_HAND_ITEM_NBT_BLACKLIST));
        }
        else {
            errorMissing(id, STRING_HAND_ITEM_NBT_WHITELIST_FULL + " or " + STRING_HAND_ITEM_NBT_BLACKLIST_FULL);
        }

        assert nbtBlackList != null;
        assert nbtWhiteList != null;
        return Map.entry(nbtWhiteList, nbtBlackList);
    }

    // Inner Work ( Block )

    private static PtaBlock createBlock(ResourceLocation id, JsonObject json) {
        if (!json.has(STRING_BLOCK) || !json.get(STRING_BLOCK).isJsonObject()) return PtaBlock.createAir();

        JsonObject blockJson = GsonHelper.getAsJsonObject(json, STRING_BLOCK);

        Set<Block> blockSet = new HashSet<>();
        Set<Fluid> fluidSet = new HashSet<>();

        // Block / Fluid Set
        if (blockJson.has(STRING_BLOCK_BLOCK)) {
            blockSet.add(BlockChecker.getExistingBlock(GsonHelper.getAsString(blockJson, STRING_BLOCK_BLOCK)));
        }
        else if (blockJson.has(STRING_BLOCK_FLUID)) {
            fluidSet.add(FluidChecker.getExistingFluid(GsonHelper.getAsString(blockJson, STRING_BLOCK_FLUID)));
        }
        else if (blockJson.has(STRING_BLOCK_TAG)) {
            String tag = GsonHelper.getAsString(blockJson, STRING_BLOCK_TAG);
            if (BlockChecker.isBlockTagExisting(tag)) {
                blockSet.addAll(BlockChecker.getBlocksForTag(tag));
            }
            else if (FluidChecker.isFluidTagExisting(tag)) {
                fluidSet.addAll(FluidChecker.getFluidsForTag(tag));
            }
            else {
                errorFormat(id, "Tag Invalid : " +  STRING_BLOCK_TAG_FULL);
            }
        }
        else {
            errorMissing(id, STRING_BLOCK_BLOCK_FULL + " or " + STRING_BLOCK_FLUID_FULL + " or " + STRING_BLOCK_TAG_FULL);
        }

        // State Sets
        Map.Entry<Set<PtaStateRecord<?>>, Set<PtaStateRecord<?>>> states = Map.entry(new HashSet<>(), new HashSet<>());
        if ((!blockSet.isEmpty() || !fluidSet.isEmpty()) && blockJson.has(STRING_BLOCK_STATE)) {
            states = createBlockStates(id, GsonHelper.getAsJsonObject(blockJson, STRING_BLOCK_STATE), blockSet.isEmpty() ? fluidSet : blockSet);
        }

        // NBTs CompoundTags
        Map.Entry<CompoundTag, CompoundTag> nbts = Map.entry(new CompoundTag(), new CompoundTag());
        if ((!blockSet.isEmpty() || !fluidSet.isEmpty()) && blockJson.has(STRING_BLOCK_NBT)) {
            nbts = createBlockNBTs(id, GsonHelper.getAsJsonObject(blockJson, STRING_BLOCK_NBT));
        }

        // Block Creation
        if (!blockSet.isEmpty())
            return PtaBlock.createBlock(blockSet, states.getKey(), states.getValue(), nbts.getKey(), nbts.getValue());
        else if (!fluidSet.isEmpty())
            return PtaBlock.createFluid(fluidSet, states.getKey(), states.getValue(), nbts.getKey(), nbts.getValue());
        else
            return PtaBlock.createAir();
    }

    private static Map.Entry<Set<PtaStateRecord<?>>, Set<PtaStateRecord<?>>> createBlockStates(ResourceLocation id, JsonObject json, Set<?> entries) {
        Set<PtaStateRecord<?>> stateWhiteList = new HashSet<>(), stateBlackList = new HashSet<>();

        if (json.has(STRING_BLOCK_STATE_WHITELIST)) {
            stateWhiteList = createStateSet(id, GsonHelper.getAsJsonObject(json, STRING_BLOCK_STATE_WHITELIST), entries);
        }
        else if (json.has(STRING_BLOCK_STATE_BLACKLIST)) {
            stateBlackList = createStateSet(id, GsonHelper.getAsJsonObject(json, STRING_BLOCK_STATE_BLACKLIST), entries);
        }
        else {
            errorMissing(id, STRING_BLOCK_STATE_WHITELIST_FULL + " or " + STRING_BLOCK_STATE_BLACKLIST_FULL);
        }

        return Map.entry(stateWhiteList, stateBlackList);
    }

    private static Map.Entry<CompoundTag, CompoundTag> createBlockNBTs(ResourceLocation id, JsonObject json) {
        CompoundTag nbtWhiteList = new CompoundTag(), nbtBlackList = new CompoundTag();

        if (json.has(STRING_BLOCK_NBT_WHITELIST)) {
            nbtWhiteList = getNbtFromString(id, GsonHelper.getAsJsonObject(json, STRING_BLOCK_NBT_WHITELIST));
        }
        else if (json.has(STRING_BLOCK_NBT_BLACKLIST)) {
            nbtBlackList = getNbtFromString(id, GsonHelper.getAsJsonObject(json, STRING_BLOCK_NBT_BLACKLIST));
        }
        else {
            errorMissing(id, STRING_BLOCK_NBT_WHITELIST_FULL + " or " + STRING_BLOCK_NBT_BLACKLIST_FULL);
        }

        assert nbtBlackList != null;
        assert nbtWhiteList != null;
        return Map.entry(nbtWhiteList, nbtBlackList);
    }

    private static Set<PtaStateRecord<?>> createStateSet(ResourceLocation id, JsonObject json, Set<?> entries) {
        Set<PtaStateRecord<?>> states = new HashSet<>();

        for (var key : json.keySet()) {
            String value = GsonHelper.getAsString(json, key);

            Property<?> p = null;
            for(var val : entries) {
                if (val instanceof Fluid) {
                    p = getPropertyByName(((Fluid) val).defaultFluidState(), key);
                }
                else if (val instanceof Block) {
                    p = getPropertyByName(((Block) val).defaultBlockState(), key);
                }

                if (p == null) break;
            }

            if (p != null) {
                addStateEntry(states, p, value);
            }
        }

        return states;
    }

    // Inner Work ( Transformation )

    private static PtaTransformation createTransformation(ResourceLocation id, JsonObject json) {
        if (!json.has(STRING_TRANSFORMATION) || !json.get(STRING_TRANSFORMATION).isJsonObject()) return PtaTransformation.createAir(0, null, null);

        // Json
        JsonObject transformationJson = GsonHelper.getAsJsonObject(json, STRING_TRANSFORMATION);

        // Chance
        if (!transformationJson.has(STRING_TRANSFORMATION_CHANCE)) {
            errorMissing(id, STRING_TRANSFORMATION_CHANCE_FULL);
            return PtaTransformation.createAir(0, null, null);
        }

        double chance = GsonHelper.getAsDouble(transformationJson, STRING_TRANSFORMATION_CHANCE);
        if (chance <= 0) return PtaTransformation.createAir(0, null, null);

        // Block
        Block block = null;
        Fluid fluid = null;
        if (transformationJson.has(STRING_TRANSFORMATION_BLOCK)) {
            block = BlockChecker.getExistingBlock(GsonHelper.getAsString(transformationJson, STRING_TRANSFORMATION_BLOCK));
            if (block.equals(Blocks.AIR)) block = null;
        }
        else if (transformationJson.has(STRING_TRANSFORMATION_FLUID)) {
            fluid = FluidChecker.getExistingFluid(GsonHelper.getAsString(transformationJson, STRING_TRANSFORMATION_FLUID));
        }

        // State
        Set<PtaStateRecord<?>> state = new HashSet<>();
        if ((block != null || fluid != null) && transformationJson.has(STRING_TRANSFORMATION_STATE)) {
            state = createStateSet(id, GsonHelper.getAsJsonObject(transformationJson, STRING_BLOCK_STATE), Set.of(block == null ? fluid : block));
        }

        // NBT
        CompoundTag nbt = new CompoundTag();
        if ((block != null || fluid != null) && transformationJson.has(STRING_TRANSFORMATION_NBT)) {
            nbt = getNbtFromString(id, GsonHelper.getAsJsonObject(transformationJson, STRING_TRANSFORMATION_NBT));
        }

        SoundEvent sound = null;
        if (transformationJson.has(STRING_TRANSFORMATION_SOUND)) {

            String soundName = GsonHelper.getAsString(transformationJson, STRING_TRANSFORMATION_SOUND);
            sound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(soundName));
        }

        // Particles
        ParticleOptions particle = null;
        if (transformationJson.has(STRING_TRANSFORMATION_PARTICLES)) {
            String particleBlockId = GsonHelper.getAsString(transformationJson, STRING_TRANSFORMATION_PARTICLES);

            if (BlockChecker.doesBlockExist(particleBlockId)) {
                particle = new BlockParticleOption(ParticleTypes.BLOCK, BlockChecker.getExistingBlock(particleBlockId).defaultBlockState());
            }
        }

        if (block != null)
            return PtaTransformation.createBlock(chance, block, state, nbt, sound, particle);
        else if (fluid != null)
            return PtaTransformation.createFluid(chance, fluid, state, nbt, sound, particle);

        return PtaTransformation.createAir(chance, sound, particle);
    }

    // Inner Work ( Pool )

    private static PtaPool createPool(ResourceLocation id, JsonObject json) {
        Map<PtaDropRecord, Integer> pool = new HashMap<>();

        if (!json.has(STRING_POOL) || !json.get(STRING_POOL).isJsonArray()) return PtaPool.create(pool);

        JsonArray dropArray = GsonHelper.getAsJsonArray(json, STRING_POOL);

        for (JsonElement arrayValue: dropArray) {
            JsonObject dropJson = (JsonObject) arrayValue;

            // Chance
            if (!dropJson.has(STRING_POOL_CHANCE) || !dropJson.get(STRING_POOL_CHANCE).isJsonPrimitive()) continue;
            int chance = GsonHelper.getAsInt(dropJson, STRING_POOL_CHANCE);

            // Min / Max
            int min, max;
            if (dropJson.has(STRING_POOL_COUNT) && dropJson.get(STRING_POOL_COUNT).isJsonPrimitive()) {
                min = max = GsonHelper.getAsInt(dropJson, STRING_POOL_COUNT);
            }
            else {
                if (!dropJson.has(STRING_POOL_MIN) || !dropJson.get(STRING_POOL_MIN).isJsonPrimitive()) continue;
                min = GsonHelper.getAsInt(dropJson, STRING_POOL_MIN);

                if (dropJson.has(STRING_POOL_MAX) && dropJson.get(STRING_POOL_MAX).isJsonPrimitive()) {
                    max = GsonHelper.getAsInt(dropJson, STRING_POOL_MAX);
                }
                else {
                    max = min;
                }
            }

            Set<Item> items = new HashSet<>();
            if (dropJson.has(STRING_POOL_ITEM) && dropJson.get(STRING_POOL_ITEM).isJsonPrimitive()) {
                items.add(ItemChecker.getExistingItem(GsonHelper.getAsString(dropJson, STRING_POOL_ITEM)));
            }
            else if (dropJson.has(STRING_POOL_TAG) && dropJson.get(STRING_POOL_TAG).isJsonPrimitive()) {
                items.addAll(ItemChecker.getItemsForTag(GsonHelper.getAsString(dropJson, STRING_POOL_TAG)));
            }
            else {
                errorMissing(id, STRING_POOL_ITEM_FULL + " or " + STRING_POOL_TAG_FULL);
            }

            CompoundTag nbt = null;
            if (dropJson.has(STRING_POOL_NBT) && dropJson.get(STRING_POOL_NBT).isJsonObject() && !items.isEmpty()) {
                nbt = getNbtFromString(id, GsonHelper.getAsJsonObject(dropJson, STRING_POOL_NBT));
            }

            pool.put(new PtaDropRecord(items, min, max, nbt), chance);
        }

        return PtaPool.create(pool);
    }

    // Inner Work ( Util )

    private static <T extends Comparable<T>> void addStateEntry(Set<PtaStateRecord<?>> stateEntries, Property<T> property, String value) {
        T parsedValue = parsePropertyValue(property, value);
        if (value.equalsIgnoreCase(SAME_STATE) || parsedValue != null) {
            stateEntries.add(new PtaStateRecord<>(property, value));
        }
        else {
            PTALoggers.error("Failed to parse value " + value + " for property " + property.getName());
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

    private static CompoundTag getNbtFromString(ResourceLocation id, JsonObject nbtObject) {
        try {
            String nbtString = nbtObject.toString();
            String regex = "\"(-?\\d+\\.?\\d*)([dsl]?)\"";
            String cleanedNbtString = nbtString.replaceAll(regex, "$1$2");

            return TagParser.parseTag(cleanedNbtString);
        } catch (CommandSyntaxException e) {
            PTALoggers.error("Incorrect Json format for " + id.getPath() + " - Error during parsing NBTs" + e);
            return null;
        }
    }

    private static Property<?> getPropertyByName(BlockState state, String name) {
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equalsIgnoreCase(name)) {
                return property;
            }
        }
        return null;
    }

    private static Property<?> getPropertyByName(FluidState state, String name) {
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equalsIgnoreCase(name)) {
                return property;
            }
        }
        return null;
    }
}
