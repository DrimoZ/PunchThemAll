package com.drimoz.punchthemall.core.model;

import com.drimoz.punchthemall.PunchThemAll;
import com.drimoz.punchthemall.core.util.PTALoggers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Interaction {

    // Private Properties

    private final ResourceLocation id;
    private final EInteractionType type;
    private Block interactedBlock;
    private ItemStack handItem;
    private Map<ItemStack, Integer> dropPool;

    private Random random;

    // Life Cycle

    public Interaction(String id, EInteractionType type) {
        this(new ResourceLocation(PunchThemAll.MOD_ID, id), type);
    }

    public Interaction(ResourceLocation id, EInteractionType type) {
        this(id, type, null, null, null);
    }

    public Interaction(
            String id, EInteractionType type,
            Block interactedBlock, ItemStack handItem,
            Map<ItemStack, Integer> dropPool
    ) {
        this(new ResourceLocation(PunchThemAll.MOD_ID, id), type, interactedBlock, handItem, dropPool);
    }

    public Interaction(
            ResourceLocation id, EInteractionType type,
            Block interactedBlock, ItemStack handItem,
            Map<ItemStack, Integer> dropPool
    ) {
        this.id = id;
        this.type = type;
        setInteractedBlock(interactedBlock);
        setHandItem(handItem);
        setDropPool(dropPool);
    }

    // Interface


    public ResourceLocation getId() {
        return id;
    }

    public EInteractionType getType() {
        return type;
    }

    public Block getInteractedBlock() {
        return interactedBlock;
    }

    public void setInteractedBlock(Block block) {
        this.interactedBlock = block;
    }

    public ItemStack getHandItem() {
        return handItem;
    }

    public void setHandItem(ItemStack item) {
        this.handItem = item;
    }

    public Map<ItemStack, Integer> getDropPool() {
        return dropPool;
    }

    public void setDropPool(Map<ItemStack, Integer> pool) {
        if (pool == null) {
            this.dropPool = new HashMap<>();
        }
        else {
            this.dropPool = pool.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue() < 0 ? 0 : entry.getValue()
                    ));
        }
    }

    public void addToPool(ItemStack item, int chance) {
        if (chance < 0 ) chance = 0;
        this.dropPool.put(item, chance);
    }

    public int getTotalChance () {
        return this.dropPool.values().stream().mapToInt(Integer::intValue).sum();
    }

    public ItemStack getItemForChance(int chance) {
        int cumulativeChance = 0;

        List<Map.Entry<ItemStack, Integer>> randomisedPool = new ArrayList<>(dropPool.entrySet());
        Collections.shuffle(randomisedPool);

        for (Map.Entry<ItemStack, Integer> entry : randomisedPool) {
            cumulativeChance += entry.getValue();

            if (chance <= cumulativeChance) {
                return entry.getKey();
            }
        }

        return null;
    }

    public ItemStack getRandomItem() {
        random = new Random();

        return getItemForChance(random.nextInt(getTotalChance()) + 1);
    }

    @Override
    public String toString() {
        return "Interaction{" +
                "id=" + id +
                ", type=" + type +
                ", interactedBlock=" + interactedBlock +
                ", handItem=" + handItem +
                ", dropPool=" + dropPool +
                ", random=" + random +
                '}';
    }
}
