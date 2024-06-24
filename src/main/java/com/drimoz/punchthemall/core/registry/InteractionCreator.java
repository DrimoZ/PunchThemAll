package com.drimoz.punchthemall.core.registry;

import com.drimoz.punchthemall.core.checker.BlockChecker;
import com.drimoz.punchthemall.core.checker.ItemChecker;
import com.drimoz.punchthemall.core.model.EInteractionType;
import com.drimoz.punchthemall.core.model.Interaction;
import com.drimoz.punchthemall.core.util.PTALoggers;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;


public class InteractionCreator {
    public static Interaction create(ResourceLocation id, JsonObject json) throws JsonSyntaxException {
        Interaction interaction = null;

        if (!json.has("type") || !json.has("drop_pool") || !json.has("interacted_block")) {
            PTALoggers.error("Incorrect Json format for " + id.getPath());
            return null;
        }

        interaction = new Interaction(id, EInteractionType.fromString(GsonHelper.getAsString(json, "type", EInteractionType.LEFT_CLICK.toString())));

        if (json.has("hand_item")) {
            String handItemString = GsonHelper.getAsString(json, "hand_item", "");

            if (!ItemChecker.doesItemExist(handItemString)) {
                PTALoggers.error("Item for Hand " + handItemString + " does not exist in the item registry.");
                return null;
            }
            else {
                interaction.setHandItem(new ItemStack(ItemChecker.getExistingItem(handItemString), 1));
            }
        }

        var dropPoolArray = GsonHelper.getAsJsonArray(json, "drop_pool");

        for(var drop: dropPoolArray) {
            JsonObject dropObject = drop.getAsJsonObject();

            if (dropObject.has("chance") && dropObject.has("item")) {
                Item poolItem = ItemChecker.getExistingItem(
                        GsonHelper.getAsString(dropObject, "item", "")
                );
                if (poolItem != null) {
                    interaction.addToPool(
                            new ItemStack(poolItem, dropObject.has("count") ? GsonHelper.getAsInt(dropObject, "count", 1): 1),
                            GsonHelper.getAsInt(dropObject, "chance", 0)
                    );
                }
            }
            else if (dropObject.has("chance") && dropObject.has("tag")) {
                String tag = GsonHelper.getAsString(dropObject, "tag", "").replace("#", "");
                Item poolItem = ItemChecker.getFirstItemFromTag(tag);

                if (poolItem != null) {
                    interaction.addToPool(
                            new ItemStack(poolItem, dropObject.has("count") ? GsonHelper.getAsInt(dropObject, "count", 1): 1),
                            GsonHelper.getAsInt(dropObject, "chance", 0)
                    );
                }
            }
        }

        JsonObject interactedBlock = json.getAsJsonObject("interacted_block");
        if (!interactedBlock.has("base_block")) {
            PTALoggers.error("Incorrect Json format for " + id.getPath() + " - Missing base_block");
            return null;
        }

        JsonObject baseBlock = interactedBlock.getAsJsonObject("base_block");
        if (baseBlock.has("block")) {
            Block block = BlockChecker.getExistingBlock(GsonHelper.getAsString(baseBlock, "block"));
            if (block == null) {
                PTALoggers.error("Block " + GsonHelper.getAsString(baseBlock, "block") + " does not exist in the block registry.");
                return null;
            }

            interaction.setInteractedBlock(block);
        }
        else if (baseBlock.has("tag")) {
            Block block = BlockChecker.getFirstBlockFromTag(GsonHelper.getAsString(baseBlock, "tag"));
            if (block == null) {
                PTALoggers.error("No Block for tag " + GsonHelper.getAsString(baseBlock, "tag") + " exists in the block registry.");
                return null;
            }

            interaction.setInteractedBlock(block);
        }
        else {
            PTALoggers.error("Incorrect Json format for " + id.getPath() + " - Mismatch base_block format");
            return null;
        }



        return interaction;
    }



}
