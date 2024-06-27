package com.drimoz.punchthemall.core.model;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class Interaction {

    // Private Properties

    private final ResourceLocation id;
    private final EInteractionType interactionType;

    private final InteractionHand interactionHand;
    private final InteractedBlock interactedBlock;

    private final List<DropEntry> dropPool;

    private final HashSet<String> biomeList;
    private final boolean biomeWhitelist;

    private Random random;

    // Life Cycle

    public Interaction(ResourceLocation id, EInteractionType interactionType, InteractionHand interactionHand, InteractedBlock interactedBlock, List<DropEntry> dropPool, HashSet<String> biomeList, boolean biomeWhitelist) {
        if (id == null) throw new IllegalArgumentException("Missing id for Interaction");
        if (interactionType == null) throw new IllegalArgumentException("Missing type for Interaction");
        if (dropPool == null || dropPool.isEmpty()) throw new IllegalArgumentException("Missing drop_pool for Interaction");

        this.id = id;
        this.interactionType = interactionType;
        this.interactionHand = interactionHand;
        this.interactedBlock = interactedBlock;
        this.dropPool = dropPool;

        this.biomeList = biomeList != null ? biomeList : new HashSet<>();
        this.biomeWhitelist = biomeWhitelist;
    }

    // Interface ( Getters )

    public ResourceLocation getId() {
        return id;
    }

    public EInteractionType getInteractionType() {
        return interactionType;
    }

    public InteractedBlock getInteractedBlock() {
        return interactedBlock;
    }

    public InteractionHand getInteractionHand() {
        return interactionHand;
    }


    public List<DropEntry> getDropPool() {
        return dropPool;
    }

    public HashSet<String> getBiomes() {
        return this.biomeList;
    }

    public boolean isBiomeWhitelist() {
        return biomeWhitelist;
    }

    // Interface ( Drop Pool )

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

    // Interface ( Others )

    public boolean isAir() {
        return this.getInteractedBlock().isAir();
    }

    public boolean isTransformedAir() {
        return this.getInteractedBlock().isTransformedAir();
    }

    public int getJeiRowCount() {
        return (int) Math.ceil(dropPool.size() / 9.0);
    }

    @Override
    public String toString() {
        return "Interaction{" +
                "\n\tid=" + id +
                ", \n\ttype=" + interactionType +
                ", \n\tinteractionHand=" + interactionHand +
                ", \n\tinteractedBlock=" + interactedBlock +
                ", \n\tdropPool=" + dropPool +
                ", \n\trandom=" + random +
                '}';
    }


}
