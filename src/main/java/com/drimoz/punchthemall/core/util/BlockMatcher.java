package com.drimoz.punchthemall.core.util;

import com.drimoz.punchthemall.core.model.classes.PtaBlock;
import com.drimoz.punchthemall.core.model.records.PtaStateRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

/**
 * Does the block at a position satisfy a {@link PtaBlock} selector?
 *
 * <p>Shared by the two places that ask: the registry, matching the clicked block against an
 * interaction's {@code target}, and the transformation pipeline, matching a destination against a
 * {@code require}. Both need the same block/fluid-set and state-list rules, and having them agree
 * matters — an author who learns the syntax on {@code target} expects it to mean the same thing on
 * {@code require}.</p>
 */
public final class BlockMatcher {

    private BlockMatcher() {}

    /** Block or fluid membership plus the state whitelist and blacklist. NBT filters are not covered. */
    public static boolean matchesBlockAndState(PtaBlock selector, Level level, BlockPos pos) {
        BlockState blockState = level.getBlockState(pos);
        FluidState fluidState = level.getFluidState(pos);
        return matchesBlockAndState(selector, blockState, fluidState);
    }

    public static boolean matchesBlockAndState(PtaBlock selector, BlockState blockState, FluidState fluidState) {
        Block block = blockState.getBlock();
        Fluid fluid = fluidState.getType();

        if (!selector.isBlockFromSet(block) && !selector.isFluidFromSet(fluid)) {
            return false;
        }

        for (PtaStateRecord<?> stateRecord : selector.getStateWhiteList()) {
            if (!matchesState(blockState, fluidState, stateRecord, selector.isBlock())) {
                return false;
            }
        }

        for (PtaStateRecord<?> stateRecord : selector.getStateBlackList()) {
            if (matchesState(blockState, fluidState, stateRecord, selector.isBlock())) {
                return false;
            }
        }

        return true;
    }

    public static boolean matchesState(BlockState blockState, FluidState fluidState, PtaStateRecord<?> stateRecord, boolean isBlock) {
        if (isBlock) {
            return blockState.getProperties().contains(stateRecord.property())
                    && blockState.getValue(stateRecord.property()).equals(stateRecord.getValue());
        }
        return fluidState.getProperties().contains(stateRecord.property())
                && fluidState.getValue(stateRecord.property()).equals(stateRecord.getValue());
    }
}
