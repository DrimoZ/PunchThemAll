package com.drimoz.punchthemall.core.model;

import com.drimoz.punchthemall.PunchThemAll;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.*;
import java.util.stream.Collectors;

public class Interaction {

    // Private Properties

    private final ResourceLocation id;
    private final EInteractionType type;

    private InteractionHand interactionHand;
    private InteractedBlock interactedBlock;

    private List<DropEntry> dropPool;

    private Random random;

    // Life Cycle

    public Interaction(ResourceLocation id, EInteractionType type) {
        this(id, type, null, null, new ArrayList<>());
    }

    public Interaction(
            ResourceLocation id, EInteractionType type,
            EInteractionHand hand_type,
            EInteractionBlock block_type, Object block,
            List<DropEntry> dropPool
    ) {
        this(id, type, new InteractionHand(hand_type), new InteractedBlock(block_type, block), dropPool);
    }

    public Interaction(
            ResourceLocation id, EInteractionType type,
            EInteractionHand hand_type, ItemStack hand_item, boolean damageable,
            EInteractionBlock block_type, Object block,
            List<DropEntry> dropPool
    ) {
        this(id, type, new InteractionHand(hand_type, hand_item, damageable), new InteractedBlock(block_type, block), dropPool);
    }

    public Interaction(
            ResourceLocation id, EInteractionType type,
            EInteractionHand hand_type,
            EInteractionBlock block_type, Object block, Double decay_chance, EInteractionBlock decay_type, Object decay_block,
            List<DropEntry> dropPool
    ) {
        this(id, type, new InteractionHand(hand_type), new InteractedBlock(block_type, block, decay_chance, decay_type, decay_block), dropPool);
    }

    public Interaction(
            ResourceLocation id, EInteractionType type,
            EInteractionHand hand_type, ItemStack hand_item, boolean damageable,
            EInteractionBlock block_type, Object block, Double decay_chance, EInteractionBlock decay_type, Object decay_block,
            List<DropEntry> dropPool
    ) {
        this(id, type, new InteractionHand(hand_type, hand_item, damageable), new InteractedBlock(block_type, block, decay_chance, decay_type, decay_block), dropPool);
    }


    public Interaction(ResourceLocation id, EInteractionType type, InteractionHand interactionHand, InteractedBlock interactedBlock, List<DropEntry> dropPool) {
        this.id = id;
        this.type = type;
        this.interactionHand = interactionHand;
        this.interactedBlock = interactedBlock;
        this.dropPool = dropPool;
    }

    // Interface

    public ResourceLocation getId() {
        return id;
    }

    public EInteractionType getType() {
        return type;
    }

    public InteractedBlock getInteractedBlock() {
        return interactedBlock;
    }

    public void setInteractedBlock(InteractedBlock interactedBlock) {
        this.interactedBlock = interactedBlock;
    }

    public InteractionHand getHandItem() {
        return interactionHand;
    }

    public void setHandItem(InteractionHand interactionHand) {
        this.interactionHand = interactionHand;
    }

    public List<DropEntry> getDropPool() {
        return dropPool;
    }

    public void setDropPool(List<DropEntry> pool) {
        if (pool == null) {
            this.dropPool = new ArrayList<>();
        }
        else {
            this.dropPool = pool.stream()
                    .map(entry -> new DropEntry(entry.getItemStack(), entry.getChance()))
                    .collect(Collectors.toList());
        }
    }

    public void addToPool(DropEntry entry) {
        this.dropPool.add(entry);
    }

    public int getTotalChance() {
        return this.dropPool.stream().mapToInt(DropEntry::getChance).sum();
    }

    public ItemStack getItemForChance(int chance) {
        int cumulativeChance = 0;

        List<DropEntry> randomisedPool = new ArrayList<>(dropPool);
        Collections.shuffle(randomisedPool);

        for (DropEntry entry : randomisedPool) {
            cumulativeChance += entry.getChance();

            if (chance <= cumulativeChance) {
                return entry.getItemStack();
            }
        }

        return ItemStack.EMPTY;
    }

    public ItemStack getRandomItem() {
        random = new Random();
        return getItemForChance(random.nextInt(getTotalChance()) + 1);
    }

    public boolean isAir() {
        return this.getInteractedBlock().getBlockAsBlock() == null;
    }

    @Override
    public String toString() {
        return "Interaction{" +
                "\nid=" + id +
                "\ntype=" + type +
                "\ninteractionHand=" + interactionHand +
                "\ninteractedBlock=" + interactedBlock +
                "\ndropPool=" + dropPool +
                "\nrandom=" + random +
                "\n}";
    }
}
