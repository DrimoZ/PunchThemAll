package com.drimoz.punchthemall.core.event;

import com.drimoz.punchthemall.core.model.EInteractionType;
import com.drimoz.punchthemall.core.model.Interaction;
import com.drimoz.punchthemall.core.model.InteractionHand;
import com.drimoz.punchthemall.core.registry.InteractionRegistry;
import com.drimoz.punchthemall.core.util.PTALoggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

public class EventHandler {

    @SubscribeEvent
    public static void onPlayerLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        PTALoggers.error("onPlayerLeftClickBlock ClientSide : " + event.getLevel().isClientSide());
        if (event.getLevel().isClientSide()) return;
        handlePlayerInteractEvent(event, EInteractionType.LEFT_CLICK);
    }

    @SubscribeEvent
    public static void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        PTALoggers.error("onPlayerRightClickBlock ClientSide : " + event.getLevel().isClientSide());
        if (event.getLevel().isClientSide()) return;
        handlePlayerInteractEvent(event, EInteractionType.RIGHT_CLICK);
    }

    @SubscribeEvent
    public static void onPlayerLeftClickItem(PlayerInteractEvent.LeftClickEmpty event) {
        PTALoggers.error("onPlayerLeftClickItem ClientSide : " + event.getLevel().isClientSide());
        if (event.getLevel().isClientSide()) return;
        handlePlayerInteractEventWithAir(event, EInteractionType.LEFT_CLICK);
    }

    @SubscribeEvent
    public static void onPlayerRightClickItem(PlayerInteractEvent.RightClickItem event) {
        PTALoggers.error("onPlayerRightClickItem ClientSide : " + event.getLevel().isClientSide());
        if (event.getLevel().isClientSide()) return;
        handlePlayerInteractEventWithAir(event, EInteractionType.RIGHT_CLICK);
    }


    private static void handlePlayerInteractEvent(PlayerInteractEvent event, EInteractionType type) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        Block block = level.getBlockState(pos).getBlock();
        Direction face = event.getFace();

        if (face == null || !InteractionRegistry.getInstance().getBlockList().contains(block)) {
            return;
        }

        boolean interactionProcessed = false;
        List<Interaction> interactions = InteractionRegistry.getInstance().getInteractionsByInteractedBlockAndType(block, type, player.isShiftKeyDown());

        for (Interaction interaction : interactions) {
            if (processInteraction(player, level, pos, face, interaction)) {
                interactionProcessed = true;
            }
        }

        if (interactionProcessed) event.setCanceled(true);
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

        if (interactionProcessed) event.setCanceled(true);
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























    private static void handlePlayerInteractEvent2(PlayerInteractEvent event, EInteractionType type) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        Block block = level.getBlockState(pos).getBlock();
        Direction face = event.getFace();
        assert face != null;

        if (InteractionRegistry.getInstance().getBlockList().contains(block)) {
            List<Interaction> interactions = InteractionRegistry.getInstance().getInteractionsByInteractedBlockAndType(block, type, player.isShiftKeyDown());

            for (Interaction interaction : interactions) {
                InteractionHand interactionHand = interaction.getHandItem();
                ItemStack playerMainHandItem = player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND);
                ItemStack playerOffHandItem = player.getItemInHand(net.minecraft.world.InteractionHand.OFF_HAND);

                if (interactionHand == null || interactionHand.getItem().isEmpty()) {
                    if (playerMainHandItem.isEmpty()) dropItem(level, pos, face, interaction.getRandomItem());
                }
                else {
                    switch (interactionHand.getHand_type()) {
                        case ANY_HAND -> {


                            if (playerMainHandItem.is(interactionHand.getItem().getItem())) {
                                dropItem(level, pos, face, interaction.getRandomItem());

                                if (interactionHand.isDamageable() && playerMainHandItem.isDamageableItem()) {
                                    useItemDurability(playerMainHandItem, player);
                                }
                            }
                            else if (playerOffHandItem.is(interactionHand.getItem().getItem())) {
                                dropItem(level, pos, face, interaction.getRandomItem());

                                if (interactionHand.isDamageable() && playerOffHandItem.isDamageableItem()) {
                                    useItemDurability(playerOffHandItem, player);
                                }
                            }
                        }
                        case MAIN_HAND -> {
                            if (playerMainHandItem.is(interactionHand.getItem().getItem())) {
                                dropItem(level, pos, face, interaction.getRandomItem());

                                if (interactionHand.isDamageable() && playerMainHandItem.isDamageableItem()) {
                                    useItemDurability(playerMainHandItem, player);
                                }
                            }
                        }
                        case OFF_HAND -> {
                            if (playerOffHandItem.is(interactionHand.getItem().getItem())) {
                                dropItem(level, pos, face, interaction.getRandomItem());

                                if (interactionHand.isDamageable() && playerOffHandItem.isDamageableItem()) {
                                    useItemDurability(playerOffHandItem, player);
                                }
                            }
                        }
                    }
                }
            }
        }
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

        PTALoggers.error("Drop Item : " + itemStack);
        ItemEntity itemEntity = new ItemEntity(level, x, y, z, itemStack.copy());
        itemEntity.setDeltaMovement(face.getStepX() * 0.1, face.getStepY() * 0.1, face.getStepZ() * 0.1);
        level.addFreshEntity(itemEntity);
    }
}
