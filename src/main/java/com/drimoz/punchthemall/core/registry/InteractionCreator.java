package com.drimoz.punchthemall.core.registry;

import com.drimoz.punchthemall.core.checker.BlockChecker;
import com.drimoz.punchthemall.core.checker.FluidChecker;
import com.drimoz.punchthemall.core.checker.ItemChecker;
import com.drimoz.punchthemall.core.model.*;
import com.drimoz.punchthemall.core.util.PTALoggers;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
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

        EInteractionType type = EInteractionType.fromString(GsonHelper.getAsString(json, "input_type"));

        InteractionHand interactionHand = null;
        if (json.has("interaction_hand")) {
            JsonObject interactionHandJson = GsonHelper.getAsJsonObject(json, "interaction_hand");
            EInteractionHand handType = EInteractionHand.fromValueOrName(GsonHelper.getAsString(interactionHandJson, "hand"));
            ItemStack handItem = ItemStack.EMPTY;
            boolean damageable = false;

            if (interactionHandJson.has("item")) {
                JsonObject itemJson = GsonHelper.getAsJsonObject(interactionHandJson, "item");
                handItem = new ItemStack(ItemChecker.getExistingItem(GsonHelper.getAsString(itemJson, "item")));
            }

            if (interactionHandJson.has("damage") && !handItem.is(ItemStack.EMPTY.getItem())) {
                damageable = GsonHelper.getAsBoolean(interactionHandJson, "damage");
            }

            interactionHand = new InteractionHand(handType, handItem, damageable);
        }

        JsonObject interactedBlockJson = GsonHelper.getAsJsonObject(json, "interacted_block");

        if (!interactedBlockJson.has("block_type")) {
            PTALoggers.error("Incorrect Json format for " + id.getPath() + " - Missing interacted_block.block_type");
            return null;
        }

        EInteractionBlock blockType = EInteractionBlock.fromString(GsonHelper.getAsString(interactedBlockJson, "block_type"));
        Object state = null, transformedState = null; // BlockState or FluidState
        Double transformationChance = null;
        EInteractionBlock transformationType = null;

        if (!blockType.equals(EInteractionBlock.AIR)) {
            if (!interactedBlockJson.has("block_base")) {
                PTALoggers.error("Incorrect Json format for " + id.getPath() + " - Missing interacted_block.block_base");
                return null;
            }

            state = getState(interactedBlockJson.getAsJsonObject("block_base"));

            if (interactedBlockJson.has("block_transformation")) {
                JsonObject blockTransformationJson = GsonHelper.getAsJsonObject(interactedBlockJson, "block_transformation");

                if (!blockTransformationJson.has("transformation_chance")) {
                    PTALoggers.error("Incorrect Json format for " + id.getPath() + " - Missing interacted_block.block_transformation.transformation_chance");
                    return null;
                }

                if (!blockTransformationJson.has("transformation_type")) {
                    PTALoggers.error("Incorrect Json format for " + id.getPath() + " - Missing interacted_block.block_transformation.transformation_type");
                    return null;
                }

                transformationChance = Double.parseDouble(GsonHelper.getAsString(blockTransformationJson, "transformation_chance", "0.0"));
                transformationType = EInteractionBlock.fromString(GsonHelper.getAsString(blockTransformationJson, "transformation_type"));

                if (!transformationType.equals(EInteractionBlock.AIR)) {
                    if (!blockTransformationJson.has("transformed_base")) {
                        PTALoggers.error("Incorrect Json format for " + id.getPath() + " - Missing interacted_block.block_transformation.transformed_base");
                        return null;
                    }

                    transformedState = getState(blockTransformationJson.getAsJsonObject("transformed_base"));
                }
            }
        }

        InteractedBlock interactedBlock = new InteractedBlock(blockType, state, transformationChance, transformationType, transformedState);

        List<DropEntry> dropPool = new ArrayList<>();
        for (var drop : GsonHelper.getAsJsonArray(json, "drop_pool")) {
            JsonObject dropObject = drop.getAsJsonObject();

            int count = 1, chance = 0;
            Item item = null;

            if (dropObject.has("item")) {
                item = ItemChecker.getExistingItem(GsonHelper.getAsString(dropObject, "item"));
            }
            else if (dropObject.has("tag")) {
                item = ItemChecker.getFirstItemFromTag(GsonHelper.getAsString(dropObject, "tag"));
            }

            if (item != null) {
                if (dropObject.has("count")) {
                    count = GsonHelper.getAsInt(dropObject, "count", 1);
                }

                if (dropObject.has("chance")) {
                    chance = GsonHelper.getAsInt(dropObject, "chance", 0);
                }

                ItemStack poolItem = new ItemStack(item, count);
                dropPool.add(new DropEntry(poolItem, chance));
            }
        }

        return new Interaction(id, type, interactionHand, interactedBlock, dropPool);
    }

    private static Object getState(JsonObject jsonObject) {
        Object state = null;

        if (jsonObject.has("block")) {
            state = BlockChecker.getExistingBlock(GsonHelper.getAsString(jsonObject, "block")).defaultBlockState();
        } else if (jsonObject.has("fluid")) {
            state = FluidChecker.getExistingFluid(GsonHelper.getAsString(jsonObject, "fluid")).defaultFluidState();
        } else {
            throw new JsonSyntaxException("Neither block nor fluid specified in block_base or transformed_base");
        }

        if (jsonObject.has("state")) {
            for (var stateItem : jsonObject.getAsJsonArray("state")) {
                JsonObject stateObject = stateItem.getAsJsonObject();


                String key = stateObject.keySet().iterator().next();
                String value = GsonHelper.getAsString(stateObject, key);

                if (state instanceof BlockState) {
                    Property<?> p = getPropertyByName((BlockState) state, key);
                    if (p != null) {
                        state = setPropertyValue((BlockState) state, p, value);
                    }
                }

                else if (state instanceof FluidState) {
                    Property<?> p = getPropertyByName((FluidState) state, key);
                    if (p != null) {
                        state = setPropertyValue((FluidState) state, p, value);
                    }
                }
            }
        }

        if (jsonObject.has("nbt")) {
            // Build NBTs
        }

        return state;
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

    private static <T extends Comparable<T>> BlockState setPropertyValue(BlockState state, Property<T> property, String value) {
        return property.getValue(value).map(valueAsComparable -> state.setValue(property, valueAsComparable)).orElse(state);
    }

    private static <T extends Comparable<T>> FluidState setPropertyValue(FluidState state, Property<T> property, String value) {
        return property.getValue(value).map(valueAsComparable -> state.setValue(property, valueAsComparable)).orElse(state);
    }
}
