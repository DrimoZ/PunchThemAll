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
        Object block = null;
        Double decayChance = null;
        EInteractionBlock decayType = null;
        Object decayBlock = null;

        if (!blockType.equals(EInteractionBlock.AIR)) {
            if (!interactedBlockJson.has("block_base")) {
                PTALoggers.error("Incorrect Json format for " + id.getPath() + " - Missing interacted_block.block_base");
                return null;
            }

            block = getBlockOrFluid(interactedBlockJson.getAsJsonObject("block_base"));

            if (interactedBlockJson.has("block_decay")) {
                JsonObject blockDecayJson = GsonHelper.getAsJsonObject(interactedBlockJson, "block_decay");

                if (!blockDecayJson.has("decay_chance")) {
                    PTALoggers.error("Incorrect Json format for " + id.getPath() + " - Missing interacted_block.block_decay.decay_chance");
                    return null;
                }

                if (!blockDecayJson.has("decay_type")) {
                    PTALoggers.error("Incorrect Json format for " + id.getPath() + " - Missing interacted_block.block_decay.decay_type");
                    return null;
                }

                decayChance = GsonHelper.getAsDouble(blockDecayJson, "decay_chance");
                decayType = EInteractionBlock.fromString(GsonHelper.getAsString(blockDecayJson, "decay_type"));

                if (!decayType.equals(EInteractionBlock.AIR)) {
                    if (!blockDecayJson.has("decay_base")) {
                        PTALoggers.error("Incorrect Json format for " + id.getPath() + " - Missing interacted_block.block_decay.decay_base");
                        return null;
                    }

                    decayBlock = getBlockOrFluid(blockDecayJson.getAsJsonObject("decay_base"));
                }
            }
        }

        InteractedBlock interactedBlock = new InteractedBlock(blockType, block, decayChance, decayType, decayBlock);

        List<DropEntry> dropPool = new ArrayList<>();
        for (var drop : GsonHelper.getAsJsonArray(json, "drop_pool")) {
            JsonObject dropObject = drop.getAsJsonObject();
            ItemStack poolItem = new ItemStack(ItemChecker.getExistingItem(GsonHelper.getAsString(dropObject, "item")));
            int chance = GsonHelper.getAsInt(dropObject, "chance", 0);
            dropPool.add(new DropEntry(poolItem, chance));
        }

        return new Interaction(id, type, interactionHand, interactedBlock, dropPool);
    }

    private static Object getBlockOrFluid(JsonObject jsonObject) {
        if (jsonObject.has("block")) {
            return BlockChecker.getExistingBlock(GsonHelper.getAsString(jsonObject, "block"));
        } else if (jsonObject.has("fluid")) {
            return FluidChecker.getExistingFluid(GsonHelper.getAsString(jsonObject, "fluid"));
        } else {
            throw new JsonSyntaxException("Neither block nor fluid specified in block_base or decay_base");
        }
    }
}
