package com.drimoz.punchthemall.core.event;

import com.drimoz.punchthemall.core.model.*;
import com.drimoz.punchthemall.core.registry.InteractionRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class EventHandler {
    private static final Map<UUID, Long> playerCooldowns = new HashMap<>();
    private static final Long cooldownInterval = 1L;

    @SubscribeEvent
    public static void onPlayerLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (isPlayerOnCooldown(event.getEntity().getUUID(), event.getEntity().tickCount)) return;
        if (event.getLevel().isClientSide()) return;
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.ABORT) return;
        handlePlayerInteractEvent(event, EInteractionType.LEFT_CLICK);
    }

    @SubscribeEvent
    public static void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isPlayerOnCooldown(event.getEntity().getUUID(), event.getEntity().tickCount)) return;
        if (event.getLevel().isClientSide()) return;
        handlePlayerInteractEvent(event, EInteractionType.RIGHT_CLICK);
    }

    @SubscribeEvent
    public static void onPlayerLeftClickItem(PlayerInteractEvent.LeftClickEmpty event) {
        if (isPlayerOnCooldown(event.getEntity().getUUID(), event.getEntity().tickCount)) return;
        if (event.getLevel().isClientSide()) return;
        handlePlayerInteractEventWithAir(event, EInteractionType.LEFT_CLICK);
    }

    @SubscribeEvent
    public static void onPlayerRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (isPlayerOnCooldown(event.getEntity().getUUID(), event.getEntity().tickCount)) return;
        if (event.getLevel().isClientSide()) return;
        handlePlayerInteractEventWithAir(event, EInteractionType.RIGHT_CLICK);
    }


    private static void handlePlayerInteractEvent(PlayerInteractEvent event, EInteractionType type) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState blockState = level.getBlockState(pos);
        FluidState fluidState = level.getFluidState(pos);
        Direction face = event.getFace();

        if (face == null) {
            return;
        }

        boolean interactionProcessed = false;
        boolean blockTransformed = false;
        List<Interaction> interactions = InteractionRegistry.getInstance().getInteractionsByInteractedBlockAndType((blockState != null ? blockState : fluidState), type, player.isShiftKeyDown());

        for (Interaction interaction : interactions) {
            if (interaction.getInteractedBlock().getBlockType() == EInteractionBlock.AIR) continue;

            if (!blockTransformed && processInteraction(player, level, pos, face, interaction)) {
                interactionProcessed = true;
                if (shouldBlockTransform(interaction.getInteractedBlock())) {
                    transformBlock(level, pos, interaction.getInteractedBlock().getState());
                    blockTransformed = true;
                }
            }
        }

        if (interactionProcessed) {
            event.setCanceled(true);
            setPlayerOnCooldown(player.getUUID(), player.tickCount);
        }
    }

    private static void handlePlayerInteractEventWithAir(PlayerInteractEvent event, EInteractionType type) {
        Player player = event.getEntity();
        Level level = event.getLevel();

        boolean interactionProcessed = false;
        List<Interaction> interactions = InteractionRegistry.getInstance().getInteractionsByInteractedBlockAndType(null, type, player.isShiftKeyDown());

        for (Interaction interaction : interactions) {
            if (processInteraction(player, level, player.blockPosition(), Direction.UP, interaction)) {
                interactionProcessed = true;
            }
        }

        if (interactionProcessed) {
            event.setCanceled(true);
            setPlayerOnCooldown(player.getUUID(), player.tickCount);
        }
    }

    private static boolean processInteraction(Player player, Level level, BlockPos pos, Direction face, Interaction interaction) {
        InteractionHand interactionHand = interaction.getHandItem();
        ItemStack playerMainHandItem = player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND);
        ItemStack playerOffHandItem = player.getItemInHand(net.minecraft.world.InteractionHand.OFF_HAND);

        if (interactionHand == null || interactionHand.getItem().isEmpty()) {
            if (playerMainHandItem.isEmpty()) {
                dropItem(level, pos, face, interaction.getRandomItem());
                return true;
            }
            return false;
        }

        switch (interactionHand.getHand_type()) {
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
        if (handItem.is(interaction.getHandItem().getItem().getItem())) {
            dropItem(level, pos, face, interaction.getRandomItem());
            if (interaction.getHandItem().isDamageable() && handItem.isDamageableItem()) {
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
        if (interactedBlock.getTransformationChance() != null) {
            double randomValue = ThreadLocalRandom.current().nextDouble();
            return randomValue <= interactedBlock.getTransformationChance();
        }
        return false;
    }

    private static void transformBlock(Level level, BlockPos pos, Object state) {
        if (state instanceof BlockState)
            level.setBlockAndUpdate(pos, (BlockState) state);
        else if (state instanceof FluidState)
            level.setBlockAndUpdate(pos, ((FluidState) state).createLegacyBlock());
    }



    // Inner work ( Player Cooldown )

    private static boolean isPlayerOnCooldown(UUID UUID, long currentTick) {
        return playerCooldowns.getOrDefault(UUID, 0L) >= currentTick - cooldownInterval;
    }

    private static void setPlayerOnCooldown(UUID UUID, long currentTick) {
        playerCooldowns.put(UUID, currentTick);
    }
}
