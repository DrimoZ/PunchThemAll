package com.drimoz.punchthemall.core.event;

import com.drimoz.punchthemall.PTAConfig;
import com.drimoz.punchthemall.core.model.classes.PtaTransformation;
import com.drimoz.punchthemall.core.model.enums.PtaDropMode;
import com.drimoz.punchthemall.core.model.records.PtaStateRecord;
import com.drimoz.punchthemall.core.util.BlockMatcher;
import com.drimoz.punchthemall.core.util.PTALoggers;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ProblemReporter;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.drimoz.punchthemall.core.registry.RegistryConstants.SAME_STATE;

/**
 * Runs an interaction's transformations: rolls them, works out where each lands, checks it is
 * allowed to touch that block, and only then changes the world.
 *
 * <p>The two phases are deliberate. Every destination is resolved and vetted against the world as it
 * stood when the click happened, before any of them is written. A transformation list would
 * otherwise mean something different depending on the order it was declared in — the second entry's
 * {@code require} would be answering a question about the first entry's handiwork — and a pack
 * author has no way to see that coming.</p>
 */
public final class TransformationApplier {

    private TransformationApplier() {}

    /**
     * A transformation that passed its roll and its checks, with the block it will act on.
     *
     * @param copied the state a {@code copy} transformation will write, read while planning so it
     *               is the block that stood there when the click happened rather than whatever an
     *               earlier entry left behind. {@code null} for everything else.
     */
    private record Planned(PtaTransformation transformation, BlockPos pos, BlockState copied) {}

    /**
     * @param origin the block the interaction happened on — the player's own position for an air
     *               interaction, since there is no block under the cursor to measure from
     * @param face   the clicked face, used by offsets in the {@code face} frame
     * @return the positions that were changed, so the caller can keep two interactions on the same
     *         click from fighting over one block
     */
    public static List<BlockPos> apply(
            Level level, Player player, BlockPos origin, Direction face,
            List<PtaTransformation> transformations, RandomSource random, Set<BlockPos> alreadyTouched
    ) {
        if (transformations.isEmpty()) return List.of();
        if (!PTAConfig.valueOrDefault(PTAConfig.INTERACTIONS.allowTransformations)) {
            logSkipped("transformations are disabled globally");
            return List.of();
        }

        List<Planned> plan = plan(level, player, origin, face, transformations, random, alreadyTouched);
        if (plan.isEmpty()) return List.of();

        List<BlockPos> applied = new ArrayList<>(plan.size());
        for (Planned planned : plan) {
            if (applyOne(level, player, planned)) {
                applied.add(planned.pos());
            }
        }
        return applied;
    }

    // Planning

    private static List<Planned> plan(
            Level level, Player player, BlockPos origin, Direction face,
            List<PtaTransformation> transformations, RandomSource random, Set<BlockPos> alreadyTouched
    ) {
        int budget = PTAConfig.valueOrDefault(PTAConfig.INTERACTIONS.maxTransformationsPerInteraction);
        Direction playerFacing = player.getDirection();

        List<Planned> plan = new ArrayList<>();
        // Positions this plan has taken. Separate from alreadyTouched, which is what earlier
        // interactions on the same click changed: two entries of one list can resolve to the same
        // block just as easily as two interactions can, and the second would be writing over a
        // world the checks were never run against.
        Set<BlockPos> claimed = new HashSet<>();

        for (PtaTransformation transformation : transformations) {
            if (plan.size() >= budget) {
                logSkipped("max_transformations_per_interaction (" + budget + ") reached");
                break;
            }
            if (!rolls(transformation, random)) continue;

            for (BlockPos pos : transformation.getOffset().resolveAll(origin, face, playerFacing)) {
                if (plan.size() >= budget) {
                    logSkipped("max_transformations_per_interaction (" + budget + ") reached");
                    break;
                }
                if (alreadyTouched.contains(pos) || claimed.contains(pos)) {
                    logSkipped("another transformation already claimed " + pos + " on this click");
                    continue;
                }
                if (!isAllowed(level, player, transformation, pos, face)) continue;

                claimed.add(pos);
                plan.add(new Planned(transformation, pos, captureCopy(level, transformation, origin, face, playerFacing)));
            }

        }
        return plan;
    }

    /**
     * The block a {@code copy} will write, read now rather than at write time.
     *
     * <p>That timing is the whole point. Pairing a copy with a break at the same place is how a
     * file moves a block; if the copy read the world when it wrote, it would find the hole the
     * break just made and move air instead.</p>
     */
    private static BlockState captureCopy(Level level, PtaTransformation transformation, BlockPos origin, Direction face, Direction playerFacing) {
        if (!transformation.isCopy()) return null;

        BlockPos source = transformation.getCopyFrom().resolve(origin, face, playerFacing);
        if (level.isOutsideBuildHeight(source) || !level.isLoaded(source)) return null;
        return level.getBlockState(source);
    }

    private static boolean rolls(PtaTransformation transformation, RandomSource random) {
        return transformation.getChance() > 0 && random.nextDouble() <= transformation.getChance();
    }

    /** Every reason a transformation may not touch a block, in the order that reads best in a log. */
    private static boolean isAllowed(Level level, Player player, PtaTransformation transformation, BlockPos pos, Direction face) {
        if (transformation.hasOffset()) {
            if (!PTAConfig.valueOrDefault(PTAConfig.INTERACTIONS.allowOffsetTransformations)) {
                return skip("offset transformations are disabled");
            }
            int cap = PTAConfig.valueOrDefault(PTAConfig.INTERACTIONS.maxTransformationOffset);
            if (transformation.getOffset().reach() > cap) {
                return skip("offset " + transformation.getOffset() + " reaches past max_transformation_offset (" + cap + ")");
            }
        }

        if (level.isOutsideBuildHeight(pos)) {
            return skip(pos + " is outside the world height");
        }
        // Never force-load: a transformation reaching into an unloaded chunk silently does nothing
        // rather than pulling terrain into memory from a click.
        if (!level.isLoaded(pos)) {
            return skip(pos + " is not loaded");
        }

        BlockState current = level.getBlockState(pos);

        switch (transformation.getOp()) {
            case BREAK -> {
                if (current.isAir()) return skip(pos + " has nothing to break");
                // Bedrock, portal frames and the like. Creative players get the vanilla exemption.
                if (current.getDestroySpeed(level, pos) < 0 && !player.getAbilities().instabuild) {
                    return skip(current.getBlock() + " at " + pos + " cannot be broken");
                }
            }
            case PLACE -> {
                if (!current.canBeReplaced()) return skip(pos + " is occupied by " + current.getBlock());
                // A block that cannot survive where it is asked to go pops on the next block update,
                // a tick or two after the click. The pack looks broken rather than the placement
                // looking refused, so refuse it here and say so.
                if (transformation.isBlock() && !transformation.getBlock().defaultBlockState().canSurvive(level, pos)) {
                    return skip(transformation.getBlock() + " cannot survive at " + pos);
                }
            }
            case REPLACE -> { /* overwrites anything by definition */ }
        }

        if (transformation.hasRequirement() && !BlockMatcher.matchesBlockAndState(transformation.getRequire(), level, pos)) {
            return skip(pos + " does not match the transformation's require filter");
        }

        return isPermitted(level, player, transformation, pos, current, face);
    }

    // Protection

    /**
     * Ask the world, and then the other mods, whether this player may change this block.
     *
     * <p>Worth more here than it would be on a plain transformation: without an offset the player is
     * changing the block they are pointing at, which vanilla already gates. With one they can reach
     * a block they never touched — over a claim boundary, from outside a protected area — so the
     * break and place events are what keeps PunchThemAll from being a way around claim mods.</p>
     */
    private static boolean isPermitted(
            Level level, Player player, PtaTransformation transformation,
            BlockPos pos, BlockState current, Direction face
    ) {
        if (!level.mayInteract(player, pos)) {
            return skip(player.getName().getString() + " may not interact with " + pos);
        }

        if (!PTAConfig.valueOrDefault(PTAConfig.INTERACTIONS.fireProtectionEvents)) return true;
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) return true;

        if (transformation.isBreak()) {
            // fireBlockBreak also applies the vanilla pre-checks — spawn protection, adventure mode,
            // game-master blocks — which is exactly right for an op that is a break.
            var event = CommonHooks.fireBlockBreak(level, serverPlayer.gameMode.getGameModeForPlayer(), serverPlayer, pos, current);
            if (event.isCanceled()) {
                return skip("a block break at " + pos + " was vetoed");
            }
            return true;
        }

        // A replace destroys what is there too, but it is not a break: routing it through
        // fireBlockBreak would inherit vanilla's canAttackBlock rule and quietly stop working for a
        // creative player holding a sword. The place event is what claim mods gate building on, and
        // it covers the same ground for this purpose.
        //
        // Fired before the write, with a snapshot of what is still there. Vanilla places first and
        // rolls back on veto; going in this order means a vetoed transformation never touches the
        // world at all, which is what the planning phase needs.
        BlockSnapshot snapshot = BlockSnapshot.create(serverLevel.dimension(), serverLevel, pos);
        if (EventHooks.onBlockPlace(serverPlayer, snapshot, face == null ? Direction.UP : face)) {
            return skip("a block place at " + pos + " was vetoed");
        }
        return true;
    }

    // Applying

    private static boolean applyOne(Level level, Player player, Planned planned) {
        PtaTransformation transformation = planned.transformation();
        BlockPos pos = planned.pos();

        if (transformation.hasParticles() && level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    transformation.getParticles(),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    25, 0.5, 0.5, 0.5, 1
            );
        }

        boolean changed = switch (transformation.getOp()) {
            // destroyBlock does the whole vanilla job: break particles, break sound, and the block's
            // own loot when asked for it. It can still decline — the block may have changed under us
            // between planning and here — so its answer is what decides the position was touched.
            case BREAK -> breakBlock(level, player, pos, transformation.getDropMode());
            case PLACE, REPLACE -> write(level, pos, transformation, planned.copied());
        };

        if (changed && transformation.hasSound()) {
            level.playSound(null, pos, transformation.getSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return changed;
    }

    /**
     * Destroy a block the way the drop mode asks for.
     *
     * <p>{@code destroyBlock} does the whole vanilla job — break particles, break sound, and the
     * block's own loot. It has no notion of a tool, though, so {@code tool} drops are rolled
     * separately with the held item and the block is then broken without dropping anything, to
     * avoid handing out both sets.</p>
     */
    private static boolean breakBlock(Level level, Player player, BlockPos pos, PtaDropMode mode) {
        if (mode != PtaDropMode.TOOL) {
            return level.destroyBlock(pos, mode.drops(), player);
        }

        BlockState state = level.getBlockState(pos);
        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        ItemStack tool = player.getMainHandItem();

        if (level instanceof ServerLevel serverLevel) {
            Block.dropResources(state, serverLevel, pos, blockEntity, player, tool);
        }
        return level.destroyBlock(pos, false, player);
    }

    /** @return whether the world was actually changed. */
    private static boolean write(Level level, BlockPos pos, PtaTransformation transformation, BlockState copied) {
        if (transformation.isCopy()) {
            // A copy of a block that was never captured — the source was unloaded, or out of the
            // world — would silently write air over the destination. Leaving it alone is the
            // honest outcome.
            if (copied == null) return skip(pos + " has nothing to copy from");
            return level.setBlockAndUpdate(pos, copied);
        }

        if (transformation.isAir()) {
            return level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }

        boolean changed;
        if (transformation.isBlock()) {
            BlockState currentState = level.getBlockState(pos);
            BlockState newState = transformation.getBlock().defaultBlockState();

            for (PtaStateRecord<?> entry : transformation.getStateList()) {
                newState = applyStateEntry(newState, entry, currentState);
            }

            changed = level.setBlockAndUpdate(pos, newState);
        } else {
            FluidState state = transformation.getFluid().defaultFluidState();

            for (PtaStateRecord<?> entry : transformation.getStateList()) {
                state = applyStateEntry(state, entry);
            }

            changed = level.setBlockAndUpdate(pos, state.createLegacyBlock());
        }

        if (changed && transformation.hasNbtList()) {
            applyNBTs(level, pos, transformation.getNbtList());
        }
        return changed;
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
        } else {
            value = parsePropertyValue(property, valueString);
        }

        if (value == null) return state;
        return state.setValue(property, value);
    }

    private static <T extends Comparable<T>> FluidState applyStateEntry(FluidState state, PtaStateRecord<T> entry) {
        Property<T> property = entry.property();
        T value = parsePropertyValue(property, entry.value());
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
            // Since 1.21.6 block entities read through a ValueInput rather than a raw CompoundTag.
            // DISCARDING keeps the behaviour the CompoundTag overload had: it reported nothing
            // either. Swap in a logging reporter if authored transformation NBT ever needs
            // diagnosing — that would be a change, so it is not made here.
            blockEntity.loadWithComponents(
                    TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), customNBT));
            blockEntity.setChanged();
        }
    }

    /**
     * A transformation that quietly does nothing is the hardest thing to debug in a pack — the file
     * looks right and the game says nothing. Every refusal above says which block and why, behind
     * the existing debug toggle.
     */
    private static boolean skip(String reason) {
        logSkipped("transformation - " + reason);
        return false;
    }

    private static void logSkipped(String reason) {
        if (PTAConfig.valueOrDefault(PTAConfig.DEBUG.logSkippedInteractions)) {
            PTALoggers.info("Skipped PunchThemAll " + reason);
        }
    }
}
