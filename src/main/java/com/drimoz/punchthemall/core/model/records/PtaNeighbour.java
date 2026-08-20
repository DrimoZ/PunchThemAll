package com.drimoz.punchthemall.core.model.records;

import com.drimoz.punchthemall.core.model.classes.PtaBlock;
import com.drimoz.punchthemall.core.util.BlockMatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * A condition on a block near the one being interacted with.
 *
 * <p>Distinct from a transformation's {@code require}, which asks about the block a transformation
 * is about to change. This asks whether the interaction should happen at all — it is the difference
 * between "only turn that block to glass if it is sand" and "only let this recipe work when there
 * is obsidian underneath". The second is what multi-block setups are built out of, and no amount of
 * {@code require} expresses it.</p>
 *
 * @param at      where to look, in any of the usual frames
 * @param block   what must (or must not) be there
 * @param invert  when true the condition holds while the block does <b>not</b> match
 */
public record PtaNeighbour(PtaOffset at, PtaBlock block, boolean invert) {

    public boolean matches(Level level, BlockPos origin, Direction face, Direction playerFacing) {
        BlockPos pos = at.resolve(origin, face, playerFacing);

        // An unloaded or out-of-world neighbour is not "not matching", it is unknown. Treating it as
        // a failure is the safe reading: a recipe should not fire because the evidence for it
        // happened to be outside the loaded world.
        if (level.isOutsideBuildHeight(pos) || !level.isLoaded(pos)) {
            return invert;
        }

        return BlockMatcher.matchesBlockAndState(block, level, pos) != invert;
    }

    @Override
    public String toString() {
        return "PtaNeighbour{" + (invert ? "not " : "") + block + " at " + at + '}';
    }
}
