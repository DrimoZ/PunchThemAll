package com.drimoz.punchthemall.core.event;

import com.drimoz.punchthemall.core.model.classes.*;
import com.drimoz.punchthemall.core.model.enums.PtaTypeEnum;
import com.drimoz.punchthemall.core.model.records.PtaStateRecord;
import com.drimoz.punchthemall.core.registry.InteractionRegistry;
import com.drimoz.punchthemall.core.util.PTALoggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class PlayerInteractionHandler {
    private static final Map<UUID, Long> PLAYER_COOLDOWNS = new HashMap<>();
    private static final Long COOLDOWN_INTERVAL = 1L;

    @SubscribeEvent
    public static void onPlayerLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (isPlayerOnCooldown(event.getEntity().getUUID(), event.getEntity().tickCount)) return;
        if (event.getLevel().isClientSide()) return;
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.ABORT) return;
        handlePlayerInteract(PtaTypeEnum.LEFT_CLICK, true, event);
    }

    @SubscribeEvent
    public static void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isPlayerOnCooldown(event.getEntity().getUUID(), event.getEntity().tickCount)) return;
        if (event.getLevel().isClientSide()) return;
        handlePlayerInteract(PtaTypeEnum.RIGHT_CLICK, true, event);
    }

    @SubscribeEvent
    public static void onPlayerLeftClickItem(PlayerInteractEvent.LeftClickEmpty event) {
        if (isPlayerOnCooldown(event.getEntity().getUUID(), event.getEntity().tickCount)) return;
        if (event.getLevel().isClientSide()) return;
        handlePlayerInteract(PtaTypeEnum.LEFT_CLICK, false, event);
    }

    @SubscribeEvent
    public static void onPlayerRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (isPlayerOnCooldown(event.getEntity().getUUID(), event.getEntity().tickCount)) return;
        if (event.getLevel().isClientSide()) return;
        handlePlayerInteract(PtaTypeEnum.RIGHT_CLICK, false, event);
    }

    private static void handlePlayerInteract(
            PtaTypeEnum type, boolean clickOnBlock, PlayerInteractEvent event
    ) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos blockPos = event.getPos();
        Direction direction = event.getFace();

        if (level.isClientSide()) return;
        if (isPlayerOnCooldown(player.getUUID(), player.tickCount)) return;

        boolean interactionProcessed = false;
        boolean blockTransformed = false;

        Set<PtaInteraction> interactions = InteractionRegistry.getInstance().getFilteredInteractions(type, clickOnBlock, player, blockPos, level);
        PTALoggers.error(" Handle Interactions : " + interactions.size());

        for (PtaInteraction interaction : interactions) {
            if (interaction.getBlock().isAir()) {
                if (processInteraction(player, level, player.blockPosition(), Direction.UP, interaction)) {
                    interactionProcessed = true;
                }
            }
            else {
                if (!blockTransformed && processInteraction(player, level, blockPos, direction, interaction)) {
                    interactionProcessed = true;
                    if (shouldBlockTransform(interaction.getTransformation())) {
                        PTALoggers.error("TRANSFORMATION NEEDED");
                        transformBlock(level, blockPos, interaction.getTransformation());
                        blockTransformed = true;
                    }
                }
            }
        }


        if (interactionProcessed) {
            PTALoggers.error("Has Processed Interaction");
            event.setCanceled(true);
            setPlayerOnCooldown(player.getUUID(), player.tickCount);
        }
    }

    private static boolean processInteraction(Player player, Level level, BlockPos pos, Direction face, PtaInteraction interaction) {
        PtaHand hand = interaction.getHand();

        ItemStack playerMainHandItem = player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND);
        ItemStack playerOffHandItem = player.getItemInHand(net.minecraft.world.InteractionHand.OFF_HAND);

        if (hand.isEmpty()) {
            if (playerMainHandItem.isEmpty()) {
                dropItem(level, pos, face, interaction.getPool().getRandomItemStack());
                return true;
            }
            return false;
        }

        switch (hand.getHand()) {
            case ANY_HAND -> {
                if (tryDropItem(player, level, pos, face, interaction, playerMainHandItem) ||
                        tryDropItem(player, level, pos, face, interaction, playerOffHandItem)) {
                    return true;
                }
            }
            case MAIN_HAND -> {
                if(tryDropItem(player, level, pos, face, interaction, playerMainHandItem)) {
                    return true;
                }
            }
            case OFF_HAND -> {
                if(tryDropItem(player, level, pos, face, interaction, playerOffHandItem)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean tryDropItem(Player player, Level level, BlockPos pos, Direction face, PtaInteraction interaction, ItemStack handItem) {
        if (interaction.getHand().getItemSet().contains(handItem.getItem())) {
            if (interaction.getHand().isConsumed() && interaction.getHand().shouldConsume()) {
                consumeItem(handItem);
            }
            else if (interaction.getHand().isDamageable() && handItem.isDamageableItem() && interaction.getHand().shouldConsume()) {
                useItemDurability(handItem, player);
            }

            dropItem(level, pos, face, interaction.getPool().getRandomItemStack());
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

    private static void consumeItem(ItemStack itemStack) {
        itemStack.shrink(1);
    }

    private static void dropItem(Level level, BlockPos pos, Direction face, ItemStack itemStack) {
        double x = pos.getX() + 0.5 + face.getStepX() * 0.75;
        double y = pos.getY() + 0.5 + face.getStepY() * 0.75;
        double z = pos.getZ() + 0.5 + face.getStepZ() * 0.75;

        ItemEntity itemEntity = new ItemEntity(level, x, y, z, itemStack.copy());
        itemEntity.setDeltaMovement(face.getStepX() * 0.1, face.getStepY() * 0.1, face.getStepZ() * 0.1);
        level.addFreshEntity(itemEntity);
    }

    private static boolean shouldBlockTransform(PtaTransformation transformation) {
        if (transformation.getChance() > 0) {
            double randomValue = ThreadLocalRandom.current().nextDouble();
            return randomValue <= transformation.getChance();
        }
        return false;
    }

    private static void transformBlock(Level level, BlockPos pos, PtaTransformation transformedBlock) {
        if (transformedBlock.isAir()) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        else if (transformedBlock.isBlock()) {
            BlockState currentState = level.getBlockState(pos);
            BlockState newState = transformedBlock.getBlock().defaultBlockState();

            for (PtaStateRecord<?> entry : transformedBlock.getStateList()) {
                newState = applyStateEntry(newState, entry, currentState);
            }

            level.setBlockAndUpdate(pos, newState);
        }
        else {
            // Apply the same for fluids
            FluidState state = transformedBlock.getFluid().defaultFluidState();

            for (PtaStateRecord<?> entry : transformedBlock.getStateList()) {
                state = applyStateEntry(state, entry);
            }

            level.setBlockAndUpdate(pos, state.createLegacyBlock());
        }
    }

    /*BlockState state = transformedBlock.getBlock().defaultBlockState();

            for (PtaStateRecord<?> entry : transformedBlock.getStateList()) {
                state = applyStateEntry(state, entry);
            }

            level.setBlockAndUpdate(pos, state);*/

    private static <T extends Comparable<T>> BlockState applyStateEntry(BlockState state, PtaStateRecord<T> entry, BlockState currentState) {
        Property<T> property = entry.property();
        T value = entry.value();

        if ("copy_state_value".equals(entry.value())) {
            if (currentState.hasProperty(property)) {
                value = currentState.getValue(property);
            } else {
                return state;
            }
        }

        return state.setValue(property, value);
    }


    private static <T extends Comparable<T>> FluidState applyStateEntry(FluidState state, PtaStateRecord<T> entry) {
        Property<T> property = entry.property();
        T value = entry.value();
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
