package com.drimoz.punchthemall.core.registry;

import com.drimoz.punchthemall.core.model.EInteractionType;
import com.drimoz.punchthemall.core.model.Interaction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InteractionRegistry {

    // Private properties

    private static final InteractionRegistry INSTANCE = new InteractionRegistry();
    private final Map<ResourceLocation, Interaction> interactions = new HashMap<>();
    private final List<Block> blockList = new ArrayList<>();

    // Life cycle

    private InteractionRegistry() {}

    // Interface

    public static InteractionRegistry getInstance() {
        return INSTANCE;
    }

    public Map<ResourceLocation, Interaction> getInteractions() {
        return interactions;
    }

    public void addInteraction(Interaction interaction) {
        interactions.put(interaction.getId(), interaction);

        if (!blockList.contains(interaction.getInteractedBlock().getBlockAsBlock())) blockList.add(interaction.getInteractedBlock().getBlockAsBlock());
    }

    public Interaction getInteractionById(ResourceLocation id) {
        return interactions.get(id);
    }

    public List<Interaction> getInteractionsByInteractedBlockAndType(Block block, EInteractionType type, boolean isShiftKeyDown) {
        EInteractionType eventType = EInteractionType.getTypeFromEvent(type, isShiftKeyDown);
        List<Interaction> result = new ArrayList<>();

        for (Interaction interaction : interactions.values()) {
            Block interactionBlock = interaction.getInteractedBlock().getBlockAsBlock();
            boolean blockMatches = (block == null && interaction.isAir()) ||
                    (block != null && block.equals(interactionBlock));

            if (blockMatches && interaction.getType() == eventType) {
                result.add(interaction);
            }
        }
        return result;
    }

    public List<Block> getBlockList() {
        return blockList;
    }

    // Inner work
}
