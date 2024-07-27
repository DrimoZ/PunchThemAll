package com.drimoz.punchthemall.core.event;

import com.drimoz.punchthemall.core.model.classes.*;
import com.drimoz.punchthemall.core.model.enums.PtaTypeEnum;
import com.drimoz.punchthemall.core.model.records.PtaStateRecord;
import com.drimoz.punchthemall.core.registry.InteractionRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static com.drimoz.punchthemall.core.registry.RegistryConstants.SAME_STATE;

public class PlayerInteractionHandler {
    private static final Map<UUID, Long> PLAYER_COOLDOWNS = new HashMap<>();
    private static final Long COOLDOWN_INTERVAL = 1L;

    private static BlockHitResult rayTrace(Level world, Player player, ClipContext.Fluid fluidMode) {
        float pitch = player.getXRot();
        float yaw = player.getYRot();
        Vec3 eyePosition = player.getEyePosition(1.0F);
        float f2 = Mth.cos(-yaw * 0.017453292F - (float)Math.PI);
        float f3 = Mth.sin(-yaw * 0.017453292F - (float)Math.PI);
        float f4 = -Mth.cos(-pitch * 0.017453292F);
        float f5 = Mth.sin(-pitch * 0.017453292F);
        float f6 = f3 * f4;
        float f7 = f2 * f4;
        double reach = player.getAttribute(ForgeMod.ENTITY_REACH.get()).getValue();
        Vec3 targetPosition = eyePosition.add((double)f6 * reach, (double)f5 * reach, (double)f7 * reach);
        return world.clip(new ClipContext(eyePosition, targetPosition, ClipContext.Block.OUTLINE, fluidMode, player));
    }

    @SubscribeEvent(priority = EventPriority.HIGH, receiveCanceled = true)
    public static void onPlayerInteract(PlayerInteractEvent event) {
        if (!(event.getEntity() instanceof FakePlayer) && isPlayerOnCooldown(event.getEntity().getUUID(), event.getEntity().tickCount)) return;
        if (event.getLevel().isClientSide()) return;

        if (event instanceof PlayerInteractEvent.LeftClickBlock leftClickBlockEvent) {
            if (!(event.getEntity() instanceof FakePlayer) && leftClickBlockEvent.getAction() != PlayerInteractEvent.LeftClickBlock.Action.ABORT) return;
            handlePlayerInteract(PtaTypeEnum.LEFT_CLICK, true, leftClickBlockEvent);
        }
        else if (event instanceof PlayerInteractEvent.LeftClickEmpty leftClickEmptyEvent) {
            handlePlayerInteract(PtaTypeEnum.LEFT_CLICK, false, leftClickEmptyEvent);
        }
        else if (event instanceof PlayerInteractEvent.RightClickBlock rightClickBlockEvent) {
            handlePlayerInteract(PtaTypeEnum.RIGHT_CLICK, true, rightClickBlockEvent);
        }
        else if (event instanceof PlayerInteractEvent.RightClickItem rightClickItemEvent) {
            handlePlayerInteract(PtaTypeEnum.RIGHT_CLICK, false, rightClickItemEvent);
        }
    }

//    @SubscribeEvent
//    public static void onPlayerLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
//        if (isPlayerOnCooldown(event.getEntity().getUUID(), event.getEntity().tickCount)) return;
//        if (event.getLevel().isClientSide()) return;
//        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.ABORT) return;
//        handlePlayerInteract(PtaTypeEnum.LEFT_CLICK, true, event);
//    }
//
//    @SubscribeEvent
//    public static void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
//        if (isPlayerOnCooldown(event.getEntity().getUUID(), event.getEntity().tickCount)) return;
//        if (event.getLevel().isClientSide()) return;
//        handlePlayerInteract(PtaTypeEnum.RIGHT_CLICK, true, event);
//    }
//
//    @SubscribeEvent
//    public static void onPlayerLeftClickItem(PlayerInteractEvent.LeftClickEmpty event) {
//        if (isPlayerOnCooldown(event.getEntity().getUUID(), event.getEntity().tickCount)) return;
//        if (event.getLevel().isClientSide()) return;
//        handlePlayerInteract(PtaTypeEnum.LEFT_CLICK, false, event);
//    }
//
//    @SubscribeEvent
//    public static void onPlayerRightClickItem(PlayerInteractEvent.RightClickItem event) {
//        if (isPlayerOnCooldown(event.getEntity().getUUID(), event.getEntity().tickCount)) return;
//        if (event.getLevel().isClientSide()) return;
//        handlePlayerInteract(PtaTypeEnum.RIGHT_CLICK, false, event);
//    }

    private static void handlePlayerInteract(PtaTypeEnum type, boolean clickOnBlock, PlayerInteractEvent event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos blockPos = event.getPos();
        Direction direction = event.getFace();

        BlockHitResult hitResult = rayTrace(level, player, ClipContext.Fluid.SOURCE_ONLY);

        if (level.isClientSide()) return;
        if (!(player instanceof FakePlayer) && isPlayerOnCooldown(player.getUUID(), player.tickCount)) return;



        boolean interactionProcessed = false;
        boolean blockTransformed = false;
        boolean fluidInteraction = false;


        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hitResult.getBlockPos();
            FluidState fluidState = level.getFluidState(pos);

            if (fluidState.getType() == Fluids.WATER || fluidState.isSource()) {
                fluidInteraction = true;
            }
        }

        Set<PtaInteraction> interactions;

        if (fluidInteraction) {
            interactions = InteractionRegistry.getInstance().getFilteredInteractions(type, clickOnBlock, player, hitResult.getBlockPos(), level);
        }
        else {
            interactions = InteractionRegistry.getInstance().getFilteredInteractions(type, clickOnBlock, player, blockPos, level);
        }

        for (PtaInteraction interaction : interactions) {
            if (interaction.getBlock().isAir()) {
                if (processInteraction(player, level, player.blockPosition(), Direction.UP, interaction)) {
                    if (!(player instanceof FakePlayer)) processPlayer(player, interaction);
                    interactionProcessed = true;
                }
            }
            else {
                if (!blockTransformed && processInteraction(player, level, fluidInteraction ? hitResult.getBlockPos() : blockPos, direction, interaction)) {
                    interactionProcessed = true;
                    if (shouldBlockTransform(interaction.getTransformation())) {
                        if (!(player instanceof FakePlayer)) processPlayer(player, interaction);
                        transformBlock(level, fluidInteraction ? hitResult.getBlockPos() : blockPos, interaction.getTransformation());
                        blockTransformed = true;
                    }
                }
            }
        }

        if (interactionProcessed) {
            event.setCanceled(true);
            if (!(player instanceof FakePlayer)) setPlayerOnCooldown(player.getUUID(), player.tickCount);
        }
    }

    private static boolean processInteraction(Player player, Level level, BlockPos pos, Direction face, PtaInteraction interaction) {
        PtaHand hand = interaction.getHand();

        ItemStack playerMainHandItem = player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND);
        ItemStack playerOffHandItem = player.getItemInHand(net.minecraft.world.InteractionHand.OFF_HAND);

        if (player instanceof FakePlayer) {
            playerOffHandItem = playerMainHandItem;
        }

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

    private static void processPlayer(Player player, PtaInteraction interaction) {
        if (interaction.hasHurtPlayer() && interaction.getHurtPlayer().shouldExecute()) {
            // Hurt player for interaction.getHurtPlayer().getValue() in Minecraft ways
            player.hurt(player.damageSources().generic(), interaction.getHurtPlayer().getValue());
        }

        if (interaction.hasConsumeFood() && interaction.getConsumeFood().shouldExecute()) {
            // Consume food (saturation / food level of the player) interaction.getConsumeFood().getValue() in Minecraft Ways
            int totalFoodPointsToConsume = interaction.getConsumeFood().getValue();

            // Access the player's FoodData instance
            FoodData foodData = player.getFoodData();

            // Attempt to consume from saturation first
            float currentSaturation = foodData.getSaturationLevel();
            float newSaturation = Math.max(currentSaturation - totalFoodPointsToConsume, 0);

            // Calculate remaining food points to consume after attempting to consume from saturation
            int remainingFoodPoints = (int) Math.ceil(totalFoodPointsToConsume - (currentSaturation - newSaturation));

            // Update saturation
            foodData.setSaturation(newSaturation);

            // If there are remaining food points to consume, subtract from food level
            int currentFoodLevel = foodData.getFoodLevel();
            int newFoodLevel = Math.max(currentFoodLevel - remainingFoodPoints, 0);

            // Update food level
            foodData.setFoodLevel(newFoodLevel);
        }
    }

    private static boolean tryDropItem(Player player, Level level, BlockPos pos, Direction face, PtaInteraction interaction, ItemStack handItem) {
        if (interaction.getHand().getItemSet().contains(handItem.getItem())) {
            if (interaction.getHand().isConsumable() && interaction.getHand().shouldConsume()) {
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

    private static void transformBlock(Level level, BlockPos pos, PtaTransformation ptaTransformation) {

        if (ptaTransformation.hasParticles()) {
            ((ServerLevel) level).sendParticles(
                    ptaTransformation.getParticles(),
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    25,
                    0.5, 0.5, 0.5,
                    1
            );
        }

        if (ptaTransformation.isAir()) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        else if (ptaTransformation.isBlock()) {
            BlockState currentState = level.getBlockState(pos);
            BlockState newState = ptaTransformation.getBlock().defaultBlockState();


            for (PtaStateRecord<?> entry : ptaTransformation.getStateList()) {
                newState = applyStateEntry(newState, entry, currentState);
            }

            level.setBlockAndUpdate(pos, newState);

            if (ptaTransformation.hasNbtList()) {
                applyNBTs(level, pos, ptaTransformation.getNbtList());
            }
        }
        else {
            FluidState state = ptaTransformation.getFluid().defaultFluidState();

            for (PtaStateRecord<?> entry : ptaTransformation.getStateList()) {
                state = applyStateEntry(state, entry);
            }

            level.setBlockAndUpdate(pos, state.createLegacyBlock());

            if (ptaTransformation.hasNbtList()) {
                applyNBTs(level, pos, ptaTransformation.getNbtList());
            }
        }

        if (ptaTransformation.hasSound()) {
            level.playSound(null, pos, ptaTransformation.getSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }


    }

    private static <T extends Comparable<T>> BlockState applyStateEntry(BlockState state, PtaStateRecord<T> entry, BlockState currentState) {
        Property<T> property = entry.property();
        String valueString = entry.value();
        T value;

        if (SAME_STATE.equalsIgnoreCase(valueString)) {
            if (currentState.hasProperty(property)) {
                value = currentState.getValue(property);
            } else {
                return state;
            }
        }
        else {
            value = parsePropertyValue(property, valueString);
        }

        if (value == null) return state;
        return state.setValue(property, value);
    }

    private static <T extends Comparable<T>> FluidState applyStateEntry(FluidState state, PtaStateRecord<T> entry) {
        Property<T> property = entry.property();
        String valueString = entry.value();
        T value = parsePropertyValue(property, valueString);
        if (value == null) return state;
        return state.setValue(property, value);
    }

    private static <T extends Comparable<T>> T parsePropertyValue(Property<T> property, String value) {
        for (T possibleValue : property.getPossibleValues()) {
            if (possibleValue.toString().equalsIgnoreCase(value)) {
                return possibleValue;
            }
        }
        return null;
    }

    private static void applyNBTs(Level level, BlockPos pos, CompoundTag customNBT) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            blockEntity.load(customNBT);
            blockEntity.setChanged();
        }
    }

    // Inner work ( Player Cooldown )

    private static boolean isPlayerOnCooldown(UUID UUID, long currentTick) {
        return PLAYER_COOLDOWNS.getOrDefault(UUID, -1L) >= currentTick - COOLDOWN_INTERVAL;
    }

    private static void setPlayerOnCooldown(UUID UUID, long currentTick) {
        PLAYER_COOLDOWNS.put(UUID, currentTick);
    }
}
