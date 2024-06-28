package com.drimoz.punchthemall.core.event;

import com.drimoz.punchthemall.core.model.*;
import com.drimoz.punchthemall.core.registry.InteractionRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class EventHandler {
    private static final Map<UUID, Long> PLAYER_COOLDOWNS = new HashMap<>();
    private static final Long COOLDOWN_INTERVAL = 1L;

    @SubscribeEvent
    public static void onPlayerLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (isPlayerOnCooldown(event.getEntity().getUUID(), event.getEntity().tickCount)) return;
        if (event.getLevel().isClientSide()) return;
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.ABORT) return;
        handlePlayerInteract(EInteractionType.LEFT_CLICK, true, event);
    }

    @SubscribeEvent
    public static void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isPlayerOnCooldown(event.getEntity().getUUID(), event.getEntity().tickCount)) return;
        if (event.getLevel().isClientSide()) return;
        handlePlayerInteract(EInteractionType.RIGHT_CLICK, true, event);
    }

    @SubscribeEvent
    public static void onPlayerLeftClickItem(PlayerInteractEvent.LeftClickEmpty event) {
        if (isPlayerOnCooldown(event.getEntity().getUUID(), event.getEntity().tickCount)) return;
        if (event.getLevel().isClientSide()) return;
        handlePlayerInteract(EInteractionType.LEFT_CLICK, false, event);
    }

    @SubscribeEvent
    public static void onPlayerRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (isPlayerOnCooldown(event.getEntity().getUUID(), event.getEntity().tickCount)) return;
        if (event.getLevel().isClientSide()) return;
        handlePlayerInteract(EInteractionType.RIGHT_CLICK, false, event);
    }

    private static void handlePlayerInteract(
            EInteractionType type, boolean clickOnBlock, PlayerInteractEvent event
    ) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos blockPos = event.getPos();
        Direction direction = event.getFace();

        if (level.isClientSide()) return;
        if (isPlayerOnCooldown(player.getUUID(), player.tickCount)) return;

        boolean interactionProcessed = false;
        boolean blockTransformed = false;

        List<Interaction> interactions = InteractionRegistry.getInstance().getFilteredInteractions(type, clickOnBlock, player, blockPos, level);


        for (Interaction interaction : interactions) {
            if (interaction.getInteractedBlock().getBlockType().equals(EInteractionBlock.AIR)) {
                if (processInteraction(player, level, player.blockPosition(), Direction.UP, interaction)) {
                    interactionProcessed = true;
                }
            }
            else {
                if (!blockTransformed && processInteraction(player, level, blockPos, direction, interaction)) {
                    interactionProcessed = true;
                    if (shouldBlockTransform(interaction.getInteractedBlock())) {
                        transformBlock(level, blockPos, interaction.getInteractedBlock().getTransformedBase());
                        blockTransformed = true;
                    }
                }
            }
        }


        if (interactionProcessed) {
            event.setCanceled(true);
            setPlayerOnCooldown(player.getUUID(), player.tickCount);
        }
    }

    private static boolean processInteraction(Player player, Level level, BlockPos pos, Direction face, Interaction interaction) {
        InteractionHand interactionHand = interaction.getInteractionHand();
        ItemStack playerMainHandItem = player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND);
        ItemStack playerOffHandItem = player.getItemInHand(net.minecraft.world.InteractionHand.OFF_HAND);

        if (interactionHand == null || interactionHand.getItemStack().isEmpty()) {
            if (playerMainHandItem.isEmpty()) {
                dropItem(level, pos, face, interaction.getRandomItem());
                return true;
            }
            return false;
        }

        switch (interactionHand.getHandType()) {
            case ANY_HAND -> {
                if (tryDropItem(player, level, pos, face, interaction, playerMainHandItem) ||
                        tryDropItem(player, level, pos, face, interaction, playerOffHandItem)) {
                    return true;
                }
                break;
            }
            case MAIN_HAND -> {
                if(tryDropItem(player, level, pos, face, interaction, playerMainHandItem)) {
                    return true;
                }
                break;
            }
            case OFF_HAND -> {
                if(tryDropItem(player, level, pos, face, interaction, playerOffHandItem)) {
                    return true;
                }
                break;
            }
        }

        return false;
    }

    private static boolean tryDropItem(Player player, Level level, BlockPos pos, Direction face, Interaction interaction, ItemStack handItem) {
        if (handItem.is(interaction.getInteractionHand().getItemStack().getItem())) {
            dropItem(level, pos, face, interaction.getRandomItem());
            if (interaction.getInteractionHand().isDamageable() && handItem.isDamageableItem()) {
                useItemDurability(handItem, player);
            }
            return true;
        }
        return false;
    }

    private static void useItemDurability(ItemStack itemStack, Player player) {
        if (itemStack.hurt(1, player.getRandom(), null)) {
            itemStack.shrink(1);
            itemStack.setDamageValue(0);
        }
    }

    private static void dropItem(Level level, BlockPos pos, Direction face, ItemStack itemStack) {
        double x = pos.getX() + 0.5 + face.getStepX() * 0.75;
        double y = pos.getY() + 0.5 + face.getStepY() * 0.75;
        double z = pos.getZ() + 0.5 + face.getStepZ() * 0.75;

        ItemEntity itemEntity = new ItemEntity(level, x, y, z, itemStack.copy());
        itemEntity.setDeltaMovement(face.getStepX() * 0.1, face.getStepY() * 0.1, face.getStepZ() * 0.1);
        level.addFreshEntity(itemEntity);
    }

    private static boolean shouldBlockTransform(InteractedBlock interactedBlock) {
        if (interactedBlock.getTransformationChance() > 0) {
            double randomValue = ThreadLocalRandom.current().nextDouble();
            return randomValue <= interactedBlock.getTransformationChance();
        }
        return false;
    }

    private static void transformBlock(Level level, BlockPos pos, InteractionBlock transformedBlock) {
        if (transformedBlock.isBlock()) {
            BlockState state = transformedBlock.getBlock().defaultBlockState();

            for (StateEntry<?> entry : transformedBlock.getStateEntries()) {
                state = applyStateEntry(state, entry);
            }

            level.setBlockAndUpdate(pos, state);
        }
        else {
            FluidState state = transformedBlock.getFluid().defaultFluidState();

            for (StateEntry<?> entry : transformedBlock.getStateEntries()) {
                state = applyStateEntry(state, entry);
            }

            level.setBlockAndUpdate(pos, state.createLegacyBlock());
        }
    }

    private static <T extends Comparable<T>> BlockState applyStateEntry(BlockState state, StateEntry<T> entry) {
        Property<T> property = entry.getProperty();
        T value = entry.getValue();
        return state.setValue(property, value);
    }

    private static <T extends Comparable<T>> FluidState applyStateEntry(FluidState state, StateEntry<T> entry) {
        Property<T> property = entry.getProperty();
        T value = entry.getValue();
        return state.setValue(property, value);
    }

    // Inner work ( Player Cooldown )

    private static boolean isPlayerOnCooldown(UUID UUID, long currentTick) {
        return PLAYER_COOLDOWNS.getOrDefault(UUID, 0L) >= currentTick - COOLDOWN_INTERVAL;
    }

    private static void setPlayerOnCooldown(UUID UUID, long currentTick) {
        PLAYER_COOLDOWNS.put(UUID, currentTick);
    }


}
