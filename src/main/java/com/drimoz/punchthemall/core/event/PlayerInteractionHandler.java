package com.drimoz.punchthemall.core.event;

import com.drimoz.punchthemall.PTAConfig;
import com.drimoz.punchthemall.core.model.classes.PtaEffect;
import com.drimoz.punchthemall.core.model.classes.PtaExtras;
import com.drimoz.punchthemall.core.model.classes.PtaHand;
import com.drimoz.punchthemall.core.model.classes.PtaInteraction;
import com.drimoz.punchthemall.core.model.classes.PtaTransformation;
import com.drimoz.punchthemall.core.model.enums.PtaHandEnum;
import com.drimoz.punchthemall.core.model.enums.PtaTypeEnum;
import com.drimoz.punchthemall.core.model.records.PtaStateRecord;
import com.drimoz.punchthemall.core.registry.InteractionRegistry;
import com.drimoz.punchthemall.core.util.PTALoggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
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
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.drimoz.punchthemall.core.registry.RegistryConstants.SAME_STATE;

public class PlayerInteractionHandler {
    private static final Map<UUID, Long> PLAYER_COOLDOWNS = new HashMap<>();
    // Guards against handling both hands of the same right-click; see onRightClickItem.
    private static final Map<UUID, Long> LAST_ITEM_CLICK_TICK = new HashMap<>();

    // NeoForge's event bus refuses listeners on the abstract PlayerInteractEvent parent, so each
    // concrete sub-event gets its own listener; they all funnel into handlePlayerInteract.

    @SubscribeEvent(priority = EventPriority.HIGH, receiveCanceled = true)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!passesCommonChecks(event)) return;
        // On the server, a left click arrives as ABORT once the swing is resolved; fake players skip that.
        if (!(event.getEntity() instanceof FakePlayer) && event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.ABORT) return;
        if (isClickTypeEnabled(PtaTypeEnum.LEFT_CLICK)) {
            handlePlayerInteract(PtaTypeEnum.LEFT_CLICK, true, event);
        }
    }

    /**
     * Server-side entry point for a left click on nothing, driven by {@code LeftClickEmptyPayload}.
     *
     * <p>{@code LeftClickEmpty} is posted by {@code Minecraft.startAttack} and never leaves the
     * client, so unlike every other click type this one cannot be observed server-side — hence the
     * payload. Nothing from the client is trusted beyond "this player swung at nothing": the hand,
     * position and every gate are re-derived here.</p>
     */
    public static void onLeftClickEmptyFromClient(Player player) {
        if (!passesCommonChecks(player, player.level())) return;
        if (alreadyHandledThisTick(player)) return;
        if (isClickTypeEnabled(PtaTypeEnum.LEFT_CLICK)) {
            handlePlayerInteract(PtaTypeEnum.LEFT_CLICK, false, player, player.level(), player.blockPosition(), null);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH, receiveCanceled = true)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!passesCommonChecks(event)) return;
        // Right-click events fire once per hand (main then off); only process the main-hand event.
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (isClickTypeEnabled(PtaTypeEnum.RIGHT_CLICK)) {
            handlePlayerInteract(PtaTypeEnum.RIGHT_CLICK, true, event);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH, receiveCanceled = true)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!passesCommonChecks(event)) return;

        // Unlike RightClickBlock, this cannot be filtered to the main hand: the client walks both
        // hands and stops at the first that consumes the click, so an item held only in the off hand
        // arrives as an OFF_HAND event — which is exactly how the "in the air" interactions are
        // authored. Both hands can still fire for one click when the main hand passes, so dedupe by
        // tick instead; handlePlayerInteract already inspects both hands itself.
        if (alreadyHandledThisTick(event.getEntity())) return;

        if (isClickTypeEnabled(PtaTypeEnum.RIGHT_CLICK)) {
            handlePlayerInteract(PtaTypeEnum.RIGHT_CLICK, false, event);
        }
    }

    private static boolean alreadyHandledThisTick(Player player) {
        long now = player.level().getGameTime();
        Long last = LAST_ITEM_CLICK_TICK.put(player.getUUID(), now);
        return last != null && last == now;
    }

    private static boolean passesCommonChecks(PlayerInteractEvent event) {
        return passesCommonChecks(event.getEntity(), event.getLevel());
    }

    private static boolean passesCommonChecks(Player entity, Level level) {
        if (!PTAConfig.INTERACTIONS.enabled.get()) {
            logSkipped("interactions are disabled globally");
            return false;
        }
        if (entity instanceof FakePlayer && !PTAConfig.PLAYERS.allowFakePlayers.get()) {
            logSkipped("fake players are disabled");
            return false;
        }
        if (isCooldownEnabledFor(entity) && isPlayerOnCooldown(entity.getUUID(), entity.level().getGameTime())) return false;
        return !level.isClientSide();
    }

    private static void handlePlayerInteract(PtaTypeEnum type, boolean clickOnBlock, PlayerInteractEvent event) {
        handlePlayerInteract(type, clickOnBlock, event.getEntity(), event.getLevel(), event.getPos(),
                event instanceof ICancellableEvent cancellable ? cancellable : null);
    }

    // `cancellable` is null when there is no event to cancel — the left-click-empty payload path.
    private static void handlePlayerInteract(PtaTypeEnum type, boolean clickOnBlock, Player player, Level level,
                                             BlockPos blockPos, ICancellableEvent cancellable) {
        BlockHitResult hitResult = rayTrace(level, player, ClipContext.Fluid.SOURCE_ONLY);
        Direction direction = getInteractionDirection(player, level, hitResult);

        if (level.isClientSide()) return;
        if (isCooldownEnabledFor(player) && isPlayerOnCooldown(player.getUUID(), level.getGameTime())) return;

        boolean interactionProcessed = false;
        boolean blockTransformed = false;
        boolean fluidInteraction = false;

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hitResult.getBlockPos();
            FluidState fluidState = level.getFluidState(pos);

            if (PTAConfig.INTERACTIONS.allowFluidInteractions.get() && (fluidState.getType() == Fluids.WATER || fluidState.isSource())) {
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

        int processedInteractions = 0;

        for (PtaInteraction interaction : interactions) {
            if (processedInteractions >= PTAConfig.INTERACTIONS.maxMatchesPerClick.get()) {
                break;
            }

            if (!isInteractionTargetEnabled(interaction)) {
                logSkipped("target type is disabled for " + interaction.getId());
                continue;
            }

            if (interaction.getBlock().isAir()) {
                if (processInteraction(player, level, player.blockPosition(), Direction.UP, interaction)) {
                    if (shouldProcessPlayerEffects(player)) processPlayer(player, interaction);
                    playInteractionFeedback(level, player.blockPosition(), interaction);
                    interactionProcessed = true;
                    processedInteractions++;
                }
            }
            else {
                BlockPos targetPos = fluidInteraction ? hitResult.getBlockPos() : blockPos;
                if (!blockTransformed && processInteraction(player, level, targetPos, direction, interaction)) {
                    interactionProcessed = true;
                    if (shouldProcessPlayerEffects(player)) processPlayer(player, interaction);
                    playInteractionFeedback(level, targetPos, interaction);
                    processedInteractions++;
                    if (PTAConfig.INTERACTIONS.allowTransformations.get() && shouldBlockTransform(interaction.getTransformation(), player.getRandom())) {
                        transformBlock(level, targetPos, interaction.getTransformation());
                        blockTransformed = true;
                    }
                }
            }
        }

        if (interactionProcessed) {
            // In NeoForge only the concrete interact events are cancellable (via ICancellableEvent).
            if (PTAConfig.INTERACTIONS.cancelVanillaInteraction.get() && cancellable != null) {
                cancellable.setCanceled(true);
            }
            if (isCooldownEnabledFor(player)) setPlayerOnCooldown(player.getUUID(), level.getGameTime());
        }
    }

    private static boolean processInteraction(Player player, Level level, BlockPos pos, Direction face, PtaInteraction interaction) {
        PtaHand hand = interaction.getHand();

        ItemStack playerMainHandItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack playerOffHandItem = player.getItemInHand(InteractionHand.OFF_HAND);

        if (player instanceof FakePlayer) {
            playerOffHandItem = playerMainHandItem;
        }

        if (hand.isEmpty()) {
            if (playerMainHandItem.isEmpty()) {
                dropRewards(player, level, pos, face, interaction, ItemStack.EMPTY);
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
                if (tryDropItem(player, level, pos, face, interaction, playerMainHandItem)) {
                    return true;
                }
            }
            case OFF_HAND -> {
                if (tryDropItem(player, level, pos, face, interaction, playerOffHandItem)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static void processPlayer(Player player, PtaInteraction interaction) {
        InteractionHand hand = InteractionHand.MAIN_HAND;
        if (interaction.getHand() != null && interaction.getHand().getHand() != null) {
            if (interaction.getHand().getHand().equals(PtaHandEnum.OFF_HAND)) hand = InteractionHand.OFF_HAND;
            player.swing(hand);
        }

        if (PTAConfig.PLAYERS.allowPlayerDamage.get() && interaction.hasHurtPlayer() && interaction.getHurtPlayer().shouldExecute(player.getRandom())) {
            player.hurt(player.damageSources().generic(), interaction.getHurtPlayer().getValue(player.getRandom()));
        }

        if (PTAConfig.PLAYERS.allowFoodConsumption.get() && interaction.hasConsumeFood() && interaction.getConsumeFood().shouldExecute(player.getRandom())) {
            int totalFoodPointsToConsume = interaction.getConsumeFood().getValue(player.getRandom());

            FoodData foodData = player.getFoodData();

            float currentSaturation = foodData.getSaturationLevel();
            float newSaturation = Math.max(currentSaturation - totalFoodPointsToConsume, 0);

            int remainingFoodPoints = (int) Math.ceil(totalFoodPointsToConsume - (currentSaturation - newSaturation));

            foodData.setSaturation(newSaturation);

            int currentFoodLevel = foodData.getFoodLevel();
            int newFoodLevel = Math.max(currentFoodLevel - remainingFoodPoints, 0);

            foodData.setFoodLevel(newFoodLevel);
        }

        if (interaction.getExtras().hasEffects()) {
            for (PtaEffect effect : interaction.getExtras().effects()) {
                if (effect.shouldApply(player.getRandom())) {
                    player.addEffect(effect.toInstance());
                }
            }
        }
    }

    private static boolean tryDropItem(Player player, Level level, BlockPos pos, Direction face, PtaInteraction interaction, ItemStack handItem) {
        if (interaction.getHand().getItemSet().contains(handItem.getItem())) {
            if (interaction.getHand().isConsumable() && interaction.getHand().shouldConsume(player.getRandom())) {
                consumeItem(handItem);
            }
            else if (interaction.getHand().isDamageable() && handItem.isDamageableItem() && interaction.getHand().shouldConsume(player.getRandom())) {
                useItemDurability(handItem, player);
            }

            dropRewards(player, level, pos, face, interaction, handItem);
            return true;
        }
        return false;
    }

    private static void dropRewards(Player player, Level level, BlockPos pos, Direction face, PtaInteraction interaction, ItemStack handItem) {
        for (ItemStack stack : interaction.getRewards().roll(player.getRandom(), handItem)) {
            dropItem(player, level, pos, face, stack);
        }
    }

    private static void playInteractionFeedback(Level level, BlockPos pos, PtaInteraction interaction) {
        PtaExtras extras = interaction.getExtras();
        if (extras.hasParticles() && level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(extras.particles(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 15, 0.4, 0.4, 0.4, 0.5);
        }
        if (extras.hasSound()) {
            level.playSound(null, pos, extras.sound(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    private static void useItemDurability(ItemStack itemStack, Player player) {
        // 1.21: durability + breaking is handled by hurtAndBreak (it shrinks the stack when broken).
        if (player.level() instanceof ServerLevel serverLevel) {
            itemStack.hurtAndBreak(1, serverLevel, player instanceof ServerPlayer sp ? sp : null, item -> {});
        }
    }

    private static void consumeItem(ItemStack itemStack) {
        itemStack.shrink(1);
    }

    private static void dropItem(Player player, Level level, BlockPos pos, Direction face, ItemStack itemStack) {
        if (!shouldInsertDropsIntoInventory(player) || !tryInsertIntoInventory(player, itemStack)) {
            double offset = PTAConfig.DROPS.dropOffset.get();
            double velocity = PTAConfig.DROPS.dropVelocity.get();
            double x = pos.getX() + 0.5 + face.getStepX() * offset;
            double y = pos.getY() + 0.5 + face.getStepY() * offset;
            double z = pos.getZ() + 0.5 + face.getStepZ() * offset;

            ItemEntity itemEntity = new ItemEntity(level, x, y, z, itemStack.copy());
            itemEntity.setDeltaMovement(face.getStepX() * velocity, face.getStepY() * velocity, face.getStepZ() * velocity);
            level.addFreshEntity(itemEntity);
        }
    }

    private static boolean tryInsertIntoInventory(Player player, ItemStack itemStack) {
        Inventory fakeInventory = player.getInventory();
        return insertItemIntoInventory(fakeInventory, itemStack);
    }

    private static boolean insertItemIntoInventory(Inventory inventory, ItemStack itemStack) {
        boolean addedToInventory = inventory.add(itemStack);
        return addedToInventory && itemStack.isEmpty();
    }

    private static boolean shouldBlockTransform(PtaTransformation transformation, RandomSource random) {
        return transformation.getChance() > 0 && random.nextDouble() <= transformation.getChance();
    }

    private static void transformBlock(Level level, BlockPos pos, PtaTransformation ptaTransformation) {
        if (ptaTransformation.hasParticles()) {
            ((ServerLevel) level).sendParticles(
                    ptaTransformation.getParticles(),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    25, 0.5, 0.5, 0.5, 1
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
            blockEntity.loadWithComponents(customNBT, level.registryAccess());
            blockEntity.setChanged();
        }
    }

    // Raytracing (uses the vanilla block interaction range attribute in 1.21)

    private static BlockHitResult rayTrace(Level world, Player player, ClipContext.Fluid fluidMode) {
        float pitch = player.getXRot();
        float yaw = player.getYRot();
        Vec3 eyePosition = player.getEyePosition(1.0F);
        float f2 = Mth.cos(-yaw * 0.017453292F - (float) Math.PI);
        float f3 = Mth.sin(-yaw * 0.017453292F - (float) Math.PI);
        float f4 = -Mth.cos(-pitch * 0.017453292F);
        float f5 = Mth.sin(-pitch * 0.017453292F);
        float f6 = f3 * f4;
        float f7 = f2 * f4;
        double reach = player.blockInteractionRange();
        Vec3 targetPosition = eyePosition.add((double) f6 * reach, (double) f5 * reach, (double) f7 * reach);
        return world.clip(new ClipContext(eyePosition, targetPosition, ClipContext.Block.OUTLINE, fluidMode, player));
    }

    private static Direction getPlayerFacingDirection(Player player, Level level) {
        if (player instanceof ServerPlayer) {
            Vec3 eyePosition = player.getEyePosition(1.0F);
            Vec3 lookVector = player.getLookAngle();

            double reachDistance = player.blockInteractionRange();
            Vec3 reachEnd = eyePosition.add(lookVector.x * reachDistance, lookVector.y * reachDistance, lookVector.z * reachDistance);

            BlockHitResult blockHitResult = level.clip(new ClipContext(eyePosition, reachEnd, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));

            if (blockHitResult.getType() == HitResult.Type.BLOCK) {
                return blockHitResult.getDirection();
            }
        }
        return null;
    }

    // Inner work ( Player Cooldown )

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent ev) {
        PLAYER_COOLDOWNS.remove(ev.getEntity().getUUID());
        LAST_ITEM_CLICK_TICK.remove(ev.getEntity().getUUID());
    }

    private static boolean isPlayerOnCooldown(UUID uuid, long currentTick) {
        int cooldownInterval = PTAConfig.INTERACTIONS.cooldownTicks.get();
        if (cooldownInterval <= 0) {
            return false;
        }
        return PLAYER_COOLDOWNS.getOrDefault(uuid, -1L) >= currentTick - cooldownInterval;
    }

    private static void setPlayerOnCooldown(UUID uuid, long currentTick) {
        if (PTAConfig.INTERACTIONS.cooldownTicks.get() > 0) {
            PLAYER_COOLDOWNS.put(uuid, currentTick);
        }
    }

    private static boolean isCooldownEnabledFor(Player player) {
        return !(player instanceof FakePlayer) || PTAConfig.PLAYERS.applyCooldownToFakePlayers.get();
    }

    private static boolean shouldProcessPlayerEffects(Player player) {
        return !(player instanceof FakePlayer) || PTAConfig.PLAYERS.applyPlayerEffectsToFakePlayers.get();
    }

    private static boolean shouldInsertDropsIntoInventory(Player player) {
        if (player instanceof FakePlayer) {
            return PTAConfig.DROPS.placeFakePlayerDropsInInventory.get();
        }
        return PTAConfig.DROPS.placeInInventory.get();
    }

    private static boolean isClickTypeEnabled(PtaTypeEnum type) {
        boolean enabled = type.isLeftClick() ? PTAConfig.INTERACTIONS.allowLeftClick.get() : PTAConfig.INTERACTIONS.allowRightClick.get();
        if (!enabled) {
            logSkipped(type + " is disabled");
        }
        return enabled;
    }

    private static boolean isInteractionTargetEnabled(PtaInteraction interaction) {
        if (interaction.getBlock().isAir()) {
            return PTAConfig.INTERACTIONS.allowAirInteractions.get();
        }
        if (interaction.getBlock().isFluid()) {
            return PTAConfig.INTERACTIONS.allowFluidInteractions.get();
        }
        return PTAConfig.INTERACTIONS.allowBlockInteractions.get();
    }

    private static void logSkipped(String reason) {
        if (PTAConfig.DEBUG.logSkippedInteractions.get()) {
            PTALoggers.info("Skipped PunchThemAll interaction: " + reason);
        }
    }

    private static Direction getInteractionDirection(Player player, Level level, BlockHitResult hitResult) {
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            return hitResult.getDirection();
        }

        Direction direction = getPlayerFacingDirection(player, level);
        return direction == null ? player.getDirection() : direction;
    }
}
