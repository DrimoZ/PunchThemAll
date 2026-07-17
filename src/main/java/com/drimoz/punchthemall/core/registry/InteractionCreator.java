package com.drimoz.punchthemall.core.registry;

import com.drimoz.punchthemall.PTAConfig;
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
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.*;

import static com.drimoz.punchthemall.core.registry.RegistryConstants.*;


public class InteractionCreator {

    public static PtaInteraction createInteraction(ResourceLocation id, JsonObject json) throws JsonSyntaxException {
        InteractionJsonReader reader = new InteractionJsonReader(id, PTALoggers::error);

        if (!reader.readBoolean(json, STRING_ENABLED, true)) {
            if (PTAConfig.DEBUG.logLoadedInteractions.get()) {
                PTALoggers.info("Skipping disabled PunchThemAll interaction " + id);
            }
            return null;
        }

        if (!json.has(STRING_TYPE)) {
            errorMissing(id, STRING_TYPE);
            return null;
        }

        PtaInteractionRecord hunger = reader.readInteractionRecord(json, STRING_HUNGER).orElse(null);
        PtaInteractionRecord damage = reader.readInteractionRecord(json, STRING_DAMAGE).orElse(null);

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
        if (reader.hasObject(json, STRING_BIOME)) {
            JsonObject biomeJson = GsonHelper.getAsJsonObject(json, STRING_BIOME);
            reader.readStringArray(biomeJson, STRING_BIOME_WHITELIST, biomeWhiteList, STRING_BIOME_WHITELIST_FULL);
            reader.readStringArray(biomeJson, STRING_BIOME_BLACKLIST, biomeBlackList, STRING_BIOME_BLACKLIST_FULL);

            if (!biomeWhiteList.isEmpty() && !biomeBlackList.isEmpty()) {
                errorFormat(id, STRING_BIOME + " cannot define both whitelist and blacklist; whitelist will take precedence at runtime");
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
        InteractionJsonReader reader = new InteractionJsonReader(id, PTALoggers::error);

        addItemsFromJson(reader, json, itemSet, STRING_HAND_ITEM_ITEM, STRING_HAND_ITEM_ITEM_FULL);
        addItemsFromJson(reader, json, itemSet, STRING_HAND_ITEM_ITEMS, STRING_HAND_ITEM_ITEMS_FULL);
        addItemTagsFromJson(reader, json, itemSet, STRING_HAND_ITEM_TAG, STRING_HAND_ITEM_TAG_FULL);
        addItemTagsFromJson(reader, json, itemSet, STRING_HAND_ITEM_TAGS, STRING_HAND_ITEM_TAGS_FULL);

        if (!reader.hasAny(json, STRING_HAND_ITEM_ITEM, STRING_HAND_ITEM_ITEMS, STRING_HAND_ITEM_TAG, STRING_HAND_ITEM_TAGS)) {
            errorMissing(id, STRING_HAND_ITEM_ITEM_FULL + " or " + STRING_HAND_ITEM_ITEMS_FULL + " or " + STRING_HAND_ITEM_TAG_FULL + " or " + STRING_HAND_ITEM_TAGS_FULL);
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
        if (!json.has(STRING_HAND_ITEM_NBT_WHITELIST) && !json.has(STRING_HAND_ITEM_NBT_BLACKLIST)) {
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
        InteractionJsonReader reader = new InteractionJsonReader(id, PTALoggers::error);
        addBlocksFromJson(reader, blockJson, blockSet, STRING_BLOCK_BLOCK, STRING_BLOCK_BLOCK_FULL);
        addBlocksFromJson(reader, blockJson, blockSet, STRING_BLOCK_BLOCKS, STRING_BLOCK_BLOCKS_FULL);
        addFluidsFromJson(reader, blockJson, fluidSet, STRING_BLOCK_FLUID, STRING_BLOCK_FLUID_FULL);
        addFluidsFromJson(reader, blockJson, fluidSet, STRING_BLOCK_FLUIDS, STRING_BLOCK_FLUIDS_FULL);
        addBlockOrFluidTagsFromJson(reader, blockJson, blockSet, fluidSet, STRING_BLOCK_TAG, STRING_BLOCK_TAG_FULL);
        addBlockOrFluidTagsFromJson(reader, blockJson, blockSet, fluidSet, STRING_BLOCK_TAGS, STRING_BLOCK_TAGS_FULL);

        if (!reader.hasAny(blockJson, STRING_BLOCK_BLOCK, STRING_BLOCK_BLOCKS, STRING_BLOCK_FLUID, STRING_BLOCK_FLUIDS, STRING_BLOCK_TAG, STRING_BLOCK_TAGS)) {
            errorMissing(id, STRING_BLOCK_BLOCK_FULL + " or " + STRING_BLOCK_BLOCKS_FULL + " or " + STRING_BLOCK_FLUID_FULL + " or " + STRING_BLOCK_FLUIDS_FULL + " or " + STRING_BLOCK_TAG_FULL + " or " + STRING_BLOCK_TAGS_FULL);
        }

        if (!blockSet.isEmpty() && !fluidSet.isEmpty()) {
            errorFormat(id, STRING_BLOCK + " cannot target blocks and fluids in the same selector; block targets will be used");
            fluidSet.clear();
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
        if (json.has(STRING_BLOCK_STATE_BLACKLIST)) {
            stateBlackList = createStateSet(id, GsonHelper.getAsJsonObject(json, STRING_BLOCK_STATE_BLACKLIST), entries);
        }
        if (!json.has(STRING_BLOCK_STATE_WHITELIST) && !json.has(STRING_BLOCK_STATE_BLACKLIST)) {
            errorMissing(id, STRING_BLOCK_STATE_WHITELIST_FULL + " or " + STRING_BLOCK_STATE_BLACKLIST_FULL);
        }

        return Map.entry(stateWhiteList, stateBlackList);
    }

    private static Map.Entry<CompoundTag, CompoundTag> createBlockNBTs(ResourceLocation id, JsonObject json) {
        CompoundTag nbtWhiteList = new CompoundTag(), nbtBlackList = new CompoundTag();

        if (json.has(STRING_BLOCK_NBT_WHITELIST)) {
            nbtWhiteList = getNbtFromString(id, GsonHelper.getAsJsonObject(json, STRING_BLOCK_NBT_WHITELIST));
        }
        if (json.has(STRING_BLOCK_NBT_BLACKLIST)) {
            nbtBlackList = getNbtFromString(id, GsonHelper.getAsJsonObject(json, STRING_BLOCK_NBT_BLACKLIST));
        }
        if (!json.has(STRING_BLOCK_NBT_WHITELIST) && !json.has(STRING_BLOCK_NBT_BLACKLIST)) {
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
            state = createStateSet(id, GsonHelper.getAsJsonObject(transformationJson, STRING_TRANSFORMATION_STATE), Set.of(block == null ? fluid : block));
        }

        // NBT
        CompoundTag nbt = new CompoundTag();
        if ((block != null || fluid != null) && transformationJson.has(STRING_TRANSFORMATION_NBT)) {
            nbt = getNbtFromString(id, GsonHelper.getAsJsonObject(transformationJson, STRING_TRANSFORMATION_NBT));
        }

        SoundEvent sound = null;
        if (transformationJson.has(STRING_TRANSFORMATION_SOUND)) {

            String soundName = GsonHelper.getAsString(transformationJson, STRING_TRANSFORMATION_SOUND);
            sound = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse(soundName));
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
        InteractionJsonReader reader = new InteractionJsonReader(id, PTALoggers::error);
        Map<PtaDropRecord, Integer> pool = new HashMap<>();

        if (!json.has(STRING_POOL)) {
            return PtaPool.create(pool);
        }
        if (!reader.hasArray(json, STRING_POOL)) {
            errorFormat(id, STRING_POOL + " must be an array");
            return PtaPool.create(pool);
        }

        JsonArray dropArray = GsonHelper.getAsJsonArray(json, STRING_POOL);

        for (int index = 0; index < dropArray.size(); index++) {
            JsonElement arrayValue = dropArray.get(index);
            if (!arrayValue.isJsonObject()) {
                errorFormat(id, STRING_POOL + "[" + index + "] must be an object");
                continue;
            }
            JsonObject dropJson = arrayValue.getAsJsonObject();
            String path = STRING_POOL + "[" + index + "]";

            // Chance
            if (!reader.hasPrimitive(dropJson, STRING_POOL_CHANCE)) {
                errorMissing(id, path + SEPARATOR + STRING_POOL_CHANCE);
                continue;
            }
            int chance = GsonHelper.getAsInt(dropJson, STRING_POOL_CHANCE);
            if (chance <= 0) {
                errorFormat(id, path + SEPARATOR + STRING_POOL_CHANCE + " must be greater than 0");
                continue;
            }

            Optional<InteractionJsonReader.Range> range = reader.readRange(dropJson, path, 0);
            if (range.isEmpty()) continue;

            Set<Item> items = new HashSet<>();
            addItemsFromJson(reader, dropJson, items, STRING_POOL_ITEM, path + SEPARATOR + STRING_POOL_ITEM);
            addItemsFromJson(reader, dropJson, items, STRING_POOL_ITEMS, path + SEPARATOR + STRING_POOL_ITEMS);
            addItemTagsFromJson(reader, dropJson, items, STRING_POOL_TAG, path + SEPARATOR + STRING_POOL_TAG);
            addItemTagsFromJson(reader, dropJson, items, STRING_POOL_TAGS, path + SEPARATOR + STRING_POOL_TAGS);
            if (!reader.hasAny(dropJson, STRING_POOL_ITEM, STRING_POOL_ITEMS, STRING_POOL_TAG, STRING_POOL_TAGS)) {
                errorMissing(id, path + SEPARATOR + STRING_POOL_ITEM + " or " + path + SEPARATOR + STRING_POOL_ITEMS + " or " + path + SEPARATOR + STRING_POOL_TAG + " or " + path + SEPARATOR + STRING_POOL_TAGS);
            }

            CompoundTag nbt = null;
            if (reader.hasObject(dropJson, STRING_POOL_NBT) && !items.isEmpty()) {
                nbt = getNbtFromString(id, GsonHelper.getAsJsonObject(dropJson, STRING_POOL_NBT));
            }

            pool.put(new PtaDropRecord(items, range.get().min(), range.get().max(), nbt), chance);
        }

        return PtaPool.create(pool);
    }


    // Inner Work ( Selectors )

    private static void addItemsFromJson(InteractionJsonReader reader, JsonObject json, Set<Item> items, String key, String path) {
        for (String itemName : reader.readStringList(json, key, path)) {
            try {
                Item item = ItemChecker.getExistingItem(itemName);
                if (item == null) {
                    PTALoggers.error(INCORRECT_FORMAT + " - " + path + " - Unknown item " + itemName);
                    continue;
                }
                items.add(item);
            } catch (RuntimeException e) {
                PTALoggers.error(INCORRECT_FORMAT + " - " + path + " - Invalid item id " + itemName + " : " + e);
            }
        }
    }

    private static void addItemTagsFromJson(InteractionJsonReader reader, JsonObject json, Set<Item> items, String key, String path) {
        for (String tagName : reader.readStringList(json, key, path)) {
            try {
                Set<Item> tagItems = ItemChecker.getItemsForTag(tagName);
                if (tagItems.isEmpty()) {
                    PTALoggers.error(INCORRECT_FORMAT + " - " + path + " - Unknown or empty item tag " + tagName);
                    continue;
                }
                items.addAll(tagItems);
            } catch (RuntimeException e) {
                PTALoggers.error(INCORRECT_FORMAT + " - " + path + " - Invalid item tag id " + tagName + " : " + e);
            }
        }
    }

    private static void addBlocksFromJson(InteractionJsonReader reader, JsonObject json, Set<Block> blocks, String key, String path) {
        for (String blockName : reader.readStringList(json, key, path)) {
            try {
                Block block = BlockChecker.getExistingBlock(blockName);
                if (block == null) {
                    PTALoggers.error(INCORRECT_FORMAT + " - " + path + " - Unknown block " + blockName);
                    continue;
                }
                blocks.add(block);
            } catch (RuntimeException e) {
                PTALoggers.error(INCORRECT_FORMAT + " - " + path + " - Invalid block id " + blockName + " : " + e);
            }
        }
    }

    private static void addFluidsFromJson(InteractionJsonReader reader, JsonObject json, Set<Fluid> fluids, String key, String path) {
        for (String fluidName : reader.readStringList(json, key, path)) {
            try {
                Fluid fluid = FluidChecker.getExistingFluid(fluidName);
                if (fluid == null) {
                    PTALoggers.error(INCORRECT_FORMAT + " - " + path + " - Unknown fluid " + fluidName);
                    continue;
                }
                fluids.add(fluid);
            } catch (RuntimeException e) {
                PTALoggers.error(INCORRECT_FORMAT + " - " + path + " - Invalid fluid id " + fluidName + " : " + e);
            }
        }
    }

    private static void addBlockOrFluidTagsFromJson(InteractionJsonReader reader, JsonObject json, Set<Block> blocks, Set<Fluid> fluids, String key, String path) {
        for (String tagName : reader.readStringList(json, key, path)) {
            try {
                boolean found = false;
                if (BlockChecker.isBlockTagExisting(tagName)) {
                    blocks.addAll(BlockChecker.getBlocksForTag(tagName));
                    found = true;
                }
                if (FluidChecker.isFluidTagExisting(tagName)) {
                    fluids.addAll(FluidChecker.getFluidsForTag(tagName));
                    found = true;
                }
                if (!found) {
                    PTALoggers.error(INCORRECT_FORMAT + " - " + path + " - Unknown block/fluid tag " + tagName);
                }
            } catch (RuntimeException e) {
                PTALoggers.error(INCORRECT_FORMAT + " - " + path + " - Invalid block/fluid tag id " + tagName + " : " + e);
            }
        }
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
