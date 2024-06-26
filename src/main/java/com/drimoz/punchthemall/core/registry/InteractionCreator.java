package com.drimoz.punchthemall.core.registry;

import com.drimoz.punchthemall.core.checker.BlockChecker;
import com.drimoz.punchthemall.core.checker.FluidChecker;
import com.drimoz.punchthemall.core.checker.ItemChecker;
import com.drimoz.punchthemall.core.model.*;
import com.drimoz.punchthemall.core.util.PTALoggers;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

import java.util.ArrayList;
import java.util.List;


public class InteractionCreator {
    public static Interaction create(ResourceLocation id, JsonObject json) throws JsonSyntaxException {
        if (!json.has("input_type")) {
            PTALoggers.error("Incorrect Json format for " + id.getPath() + " - Missing input_type");
            return null;
        }

        if (!json.has("interacted_block")) {
            PTALoggers.error("Incorrect Json format for " + id.getPath() + " - Missing interacted_block");
            return null;
        }

        if (!json.has("drop_pool")) {
            PTALoggers.error("Incorrect Json format for " + id.getPath() + " - Missing drop_pool");
            return null;
        }

        EInteractionType interactionType = EInteractionType.fromString(GsonHelper.getAsString(json, "input_type"));
        InteractionHand interactionHand = null;
        InteractedBlock interactedBlock;
        List<DropEntry> dropPool = new ArrayList<>();

        // Interaction Hand
        if (json.has("interaction_hand")) {
            interactionHand = getInteractionHand(id, GsonHelper.getAsJsonObject(json, "interaction_hand"));
            if (interactionHand == null) {
                PTALoggers.error("Incorrect Json format for " + id.getPath() + " - Error creating interaction_hand");
                return null;
            }
        }

        // Interacted Block
        interactedBlock = getInteractedBlock(id, GsonHelper.getAsJsonObject(json, "interacted_block"));
        if (interactedBlock == null) {
            PTALoggers.error("Incorrect Json format for " + id.getPath() + " - Error creating interacted_block");
            return null;
        }

        // Drop Pool
        getDropPool(id, GsonHelper.getAsJsonArray(json, "drop_pool"), dropPool);
        if (dropPool.isEmpty()) {
            PTALoggers.error("Incorrect Json format for " + id.getPath() + " - Missing Items in drop_pool");
            return null;
        }

        return new Interaction(id, interactionType, interactionHand, interactedBlock, dropPool);
    }

    private static InteractionHand getInteractionHand(ResourceLocation id, JsonObject handObject) {
        if (!handObject.has("hand")) {
            PTALoggers.error("Incorrect Json format for " + id.getPath() + " - Missing interaction_hand.hand");
            return null;
        }

        EInteractionHand handType = EInteractionHand.fromValueOrName(GsonHelper.getAsString(handObject, "hand"));
        ItemStack handItem = ItemStack.EMPTY;
        boolean damageable = false;


        if (!handType.equals(EInteractionHand.ANY_HAND) && !handObject.has("item")) {
            PTALoggers.error("Incorrect Json format for " + id.getPath() + " - Missing interaction_hand.item");
            return null;
        }

        if (handObject.has("item")) {
            JsonObject itemJson = GsonHelper.getAsJsonObject(handObject, "item");

            if (itemJson.has("item")) {
                handItem = new ItemStack(ItemChecker.getExistingItem(GsonHelper.getAsString(itemJson, "item")));
            } else if (itemJson.has("tag")) {
                Item itemFromTag = ItemChecker.getFirstItemFromTag(GsonHelper.getAsString(itemJson, "tag"));
                if (itemFromTag != null) handItem = new ItemStack(itemFromTag);
            }
            else {
                PTALoggers.error("Incorrect Json format for " + id.getPath() + " - Missing interaction_hand.item.item or interaction_hand.item.tag");
                return null;
            }

            if (handItem != ItemStack.EMPTY && itemJson.has("nbt")) {
                PTALoggers.error("With GsonHelper : " + GsonHelper.getAsJsonObject(itemJson, "nbt"));
                PTALoggers.error("Without GsonHelper : " + itemJson.get("nbt"));
                CompoundTag nbt = getNbtFromString(id, GsonHelper.getAsJsonObject(itemJson, "nbt"));
                handItem.setTag(nbt);
            }
        }

        if (handObject.has("damage") && !handItem.is(ItemStack.EMPTY.getItem())) {
            damageable = GsonHelper.getAsBoolean(handObject, "damage");
        }

        return new InteractionHand(handType, handItem, damageable);
    }

    private static InteractedBlock getInteractedBlock(ResourceLocation id, JsonObject interactedBlockObject) {
        if (!interactedBlockObject.has("block_type")) {
            PTALoggers.error("Incorrect Json format for " + id.getPath() + " - Missing interacted_block.block_type");
            return null;
        }

        EInteractionBlock transformationType = null, blockType = EInteractionBlock.fromString(GsonHelper.getAsString(interactedBlockObject, "block_type"));
        InteractionBlock blockBase = null, transformedBase = null;
        double transformationChance = 0;

        if (!blockType.equals(EInteractionBlock.AIR)) {
            if (!interactedBlockObject.has("block_base")) {
                PTALoggers.error("Incorrect Json format for " + id.getPath() + " - Missing interacted_block.block_base");
                return null;
            }


            blockBase = getInteractionBlock(id, GsonHelper.getAsJsonObject(interactedBlockObject, "block_base"));
            if (blockBase == null) {
                PTALoggers.error("Incorrect Json format for " + id.getPath() + " - Error creating interacted_block.block_base");
                return null;
            }

            if (interactedBlockObject.has("block_transformation")) {
                JsonObject blockTransformationObject = GsonHelper.getAsJsonObject(interactedBlockObject, "block_transformation");

                if (!blockTransformationObject.has("transformation_chance")) {
                    PTALoggers.error("Incorrect Json format for " + id.getPath() + " - Missing interacted_block.block_transformation.transformation_chance");
                    return null;
                }

                if (!blockTransformationObject.has("transformation_type")) {
                    PTALoggers.error("Incorrect Json format for " + id.getPath() + " - Missing interacted_block.block_transformation.transformation_type");
                    return null;
                }

                transformationChance = Double.parseDouble(GsonHelper.getAsString(blockTransformationObject, "transformation_chance", "0.0"));
                transformationType = EInteractionBlock.fromString(GsonHelper.getAsString(blockTransformationObject, "transformation_type"));

                if (!transformationType.equals(EInteractionBlock.AIR)) {
                    if (!blockTransformationObject.has("transformed_base")) {
                        PTALoggers.error("Incorrect Json format for " + id.getPath() + " - Missing interacted_block.block_transformation.transformed_base");
                        return null;
                    }

                    transformedBase = getInteractionBlock(id, GsonHelper.getAsJsonObject(blockTransformationObject, "transformed_base"));
                    if (transformedBase == null) {
                        PTALoggers.error("Incorrect Json format for " + id.getPath() + " - Error creating interacted_block.block_transformation.transformed_base");
                        return null;
                    }
                }
            }
        }

        return new InteractedBlock(blockType, blockBase, transformationChance, transformationType, transformedBase);
    }

    private static InteractionBlock getInteractionBlock(ResourceLocation id, JsonObject interactionBlockObject) {
        if (interactionBlockObject.has("block")) {
            Block block = BlockChecker.getExistingBlock(GsonHelper.getAsString(interactionBlockObject, "block"));

            if (block == null) {
                PTALoggers.error("Incorrect Json format for " + id.getPath() + " - Error creating interacted_block.block_base.block");
                return null;
            }

            List<StateEntry<?>> stateEntries = new ArrayList<>();
            CompoundTag nbt = new CompoundTag();

            if (interactionBlockObject.has("state")) {
                for (var stateItem : GsonHelper.getAsJsonArray(interactionBlockObject, "state")) {
                    JsonObject stateObject = stateItem.getAsJsonObject();

                    String key = stateObject.keySet().iterator().next();
                    String value = GsonHelper.getAsString(stateObject, key);

                    Property<?> p = getPropertyByName(block.defaultBlockState(), key);
                    if (p != null) {
                        addStateEntry(stateEntries, p, value);
                    }
                }
            }

            if (interactionBlockObject.has("nbt")) {
                nbt = getNbtFromString(id, GsonHelper.getAsJsonObject(interactionBlockObject, "nbt"));
            }

            return new InteractionBlock(block, stateEntries, nbt);

        } else if (interactionBlockObject.has("fluid")) {
            Fluid fluid = FluidChecker.getExistingFluid(GsonHelper.getAsString(interactionBlockObject, "fluid"));

            if (fluid == null) {
                PTALoggers.error("Incorrect Json format for " + id.getPath() + " - Error creating interacted_block.block_base.fluid");
                return null;
            }

            List<StateEntry<?>> stateEntries = new ArrayList<>();
            CompoundTag nbt = new CompoundTag();

            if (interactionBlockObject.has("state")) {
                for (var stateItem : interactionBlockObject.getAsJsonArray("state")) {
                    JsonObject stateObject = stateItem.getAsJsonObject();

                    String key = stateObject.keySet().iterator().next();
                    String value = GsonHelper.getAsString(stateObject, key);

                    Property<?> p = getPropertyByName(fluid.defaultFluidState(), key);
                    if (p != null) {
                        addStateEntry(stateEntries, p, value);
                    }
                }
            }

            if (interactionBlockObject.has("nbt")) {
                nbt = getNbtFromString(id, GsonHelper.getAsJsonObject(interactionBlockObject, "nbt"));
            }

            return new InteractionBlock(fluid, stateEntries, nbt);
        } else {
            PTALoggers.error("Incorrect Json format for " + id.getPath() + " - Missing interacted_block.block_base.block or interacted_block.block_base.fluid");
            return null;
        }
    }

    private static void getDropPool(ResourceLocation id, JsonArray poolArray, List<DropEntry> dropEntries) {
        for (var drop : poolArray) {
            JsonObject dropObject = drop.getAsJsonObject();
            String itemName;

            int count = 1, chance = 0;
            Item item = null;

            if (dropObject.has("item")) {
                itemName = GsonHelper.getAsString(dropObject, "item");
                item = ItemChecker.getExistingItem(itemName);
            }
            else if (dropObject.has("tag")) {
                itemName = GsonHelper.getAsString(dropObject, "tag");
                item = ItemChecker.getFirstItemFromTag(itemName);
            }
            else {
                PTALoggers.error("Incorrect Json format for " + id.getPath() + " - Error during creating pool item : " + dropObject);
                return;
            }

            if (item == null) {
                PTALoggers.error("Incorrect Json format for " + id.getPath() + " - Error could not find item or tag named " + itemName + " in drop_pool");
                return;
            }

            if (dropObject.has("count")) {
                count = GsonHelper.getAsInt(dropObject, "count", 1);
            }

            if (dropObject.has("chance")) {
                chance = GsonHelper.getAsInt(dropObject, "chance", 0);
            }

            ItemStack poolItem = new ItemStack(item, count);

            if (dropObject.has("nbt")) {
                CompoundTag nbt = getNbtFromString(id, GsonHelper.getAsJsonObject(dropObject, "nbt"));
                poolItem.setTag(nbt);
            }

            dropEntries.add(new DropEntry(poolItem, chance));
        }
    }


    private static <T extends Comparable<T>> void addStateEntry(List<StateEntry<?>> stateEntries, Property<T> property, String value) {
        T parsedValue = parsePropertyValue(property, value);
        if (parsedValue != null) {
            stateEntries.add(new StateEntry<>(property, parsedValue));
        } else {
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
            return TagParser.parseTag(nbtObject.toString());
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
