package com.drimoz.punchthemall.core.model.records;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.Locale;

/**
 * Where a transformation lands, relative to the block the interaction happened on.
 *
 * <p>{@link #NONE} means "the block itself", which is what every transformation did before offsets
 * existed.</p>
 *
 * <p>The three frames all use the same axis roles — {@code x} is right, {@code y} is up, {@code z}
 * is forward — and differ only in what "forward" and "right" point at:</p>
 *
 * <ul>
 *   <li>{@link Frame#WORLD}: the world axes. {@code x} east, {@code y} up, {@code z} south. The
 *       same numbers always land on the same side of the block, whoever clicked and from where.</li>
 *   <li>{@link Frame#PLAYER}: forward is the way the player is facing, flattened to the four
 *       horizontal directions. "One block in front of me" survives the player turning around.</li>
 *   <li>{@link Frame#FACE}: forward is the face that was clicked, so {@code z: 1} is one block out
 *       of that face — where a placed block would normally go. Right and up stay on the player, so
 *       clicking the top of a block still gives a sane left/right.</li>
 * </ul>
 */
public record PtaOffset(int x, int y, int z, Frame frame) {

    public enum Frame {
        WORLD,
        PLAYER,
        FACE;

        public static Frame fromString(String frame) {
            for (Frame value : values()) {
                if (value.name().equalsIgnoreCase(frame)) return value;
            }
            throw new IllegalArgumentException("Unknown offset frame: " + frame);
        }

        public String serialized() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final PtaOffset NONE = new PtaOffset(0, 0, 0, Frame.WORLD);

    public boolean isZero() {
        return x == 0 && y == 0 && z == 0;
    }

    /**
     * How far this reaches, as a Chebyshev distance — the largest single-axis step. That is the
     * number the {@code max_transformation_offset} config caps, because it bounds the offset inside
     * a cube around the origin regardless of which frame resolves it.
     */
    public int reach() {
        return Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z)));
    }

    /**
     * Resolve to an absolute position.
     *
     * @param origin        the block the interaction happened on
     * @param face          the clicked face, used as forward in {@link Frame#FACE}
     * @param playerFacing  the player's horizontal facing, used as forward in {@link Frame#PLAYER}
     *                      and as right in every non-world frame
     */
    public BlockPos resolve(BlockPos origin, Direction face, Direction playerFacing) {
        if (isZero()) return origin;
        if (frame == Frame.WORLD) return origin.offset(x, y, z);

        // Flatten to the horizontal plane: a player looking at their feet still has a facing, and
        // "my right" should not tip over when they look up.
        Direction horizontal = playerFacing == null || playerFacing.getAxis().isVertical()
                ? Direction.NORTH
                : playerFacing;

        Direction forward = frame == Frame.FACE && face != null ? face : horizontal;
        Direction right = horizontal.getClockWise();

        return origin
                .relative(right, x)
                .relative(Direction.UP, y)
                .relative(forward, z);
    }

    @Override
    public String toString() {
        return "PtaOffset{" + x + ", " + y + ", " + z + " (" + frame.serialized() + ")}";
    }
}
