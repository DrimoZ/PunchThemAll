package com.drimoz.punchthemall.core.model.records;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Where something lands, relative to the block the interaction happened on.
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
 *
 * <p>An offset can also describe a <b>box</b> rather than a single block, by carrying a second
 * corner in {@link #to()}. Both corners are read in the same frame and the box includes both, so a
 * 3x3 floor is one entry instead of nine.</p>
 */
public record PtaOffset(int x, int y, int z, Frame frame, PtaOffset to) {

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

    /** The far corner is a plain point: it borrows the near corner's frame and cannot nest further. */
    public PtaOffset {
        if (to != null && to.to() != null) {
            throw new IllegalArgumentException("An offset's far corner cannot itself be a box.");
        }
        if (to != null && to.frame() != frame) {
            to = new PtaOffset(to.x(), to.y(), to.z(), frame);
        }
    }

    public PtaOffset(int x, int y, int z, Frame frame) {
        this(x, y, z, frame, null);
    }

    public boolean isZero() {
        return x == 0 && y == 0 && z == 0 && !isRegion();
    }

    public boolean isRegion() {
        return to != null;
    }

    /**
     * How far this reaches, as a Chebyshev distance — the largest single-axis step, taken over both
     * corners. That is the number the {@code max_transformation_offset} config caps, because it
     * bounds the offset inside a cube around the origin regardless of which frame resolves it.
     */
    public int reach() {
        int near = Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z)));
        if (to == null) return near;
        return Math.max(near, Math.max(Math.abs(to.x()), Math.max(Math.abs(to.y()), Math.abs(to.z()))));
    }

    /** How many blocks this covers. One unless it is a box. */
    public int size() {
        if (to == null) return 1;
        return (Math.abs(to.x() - x) + 1) * (Math.abs(to.y() - y) + 1) * (Math.abs(to.z() - z) + 1);
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
        return resolve(origin, face, playerFacing, x, y, z);
    }

    /**
     * Every position this covers, in a stable order. A single block for a plain offset; the whole
     * box, corners included, for a region.
     *
     * <p>Resolved corner by corner rather than by resolving two corners and walking between them:
     * in a rotated frame the box would otherwise come out mirrored, and a pattern the author drew
     * one way would land the other way round.</p>
     */
    public List<BlockPos> resolveAll(BlockPos origin, Direction face, Direction playerFacing) {
        if (to == null) return List.of(resolve(origin, face, playerFacing));

        List<BlockPos> positions = new ArrayList<>(size());
        for (int ox = Math.min(x, to.x()); ox <= Math.max(x, to.x()); ox++) {
            for (int oy = Math.min(y, to.y()); oy <= Math.max(y, to.y()); oy++) {
                for (int oz = Math.min(z, to.z()); oz <= Math.max(z, to.z()); oz++) {
                    positions.add(resolve(origin, face, playerFacing, ox, oy, oz));
                }
            }
        }
        return positions;
    }

    private BlockPos resolve(BlockPos origin, Direction face, Direction playerFacing, int ox, int oy, int oz) {
        if (ox == 0 && oy == 0 && oz == 0) return origin;
        if (frame == Frame.WORLD) return origin.offset(ox, oy, oz);

        // Flatten to the horizontal plane: a player looking at their feet still has a facing, and
        // "my right" should not tip over when they look up.
        Direction horizontal = playerFacing == null || playerFacing.getAxis().isVertical()
                ? Direction.NORTH
                : playerFacing;

        Direction forward = frame == Frame.FACE && face != null ? face : horizontal;
        Direction right = horizontal.getClockWise();

        return origin
                .relative(right, ox)
                .relative(Direction.UP, oy)
                .relative(forward, oz);
    }

    @Override
    public String toString() {
        String near = x + ", " + y + ", " + z;
        String span = to == null ? "" : " to " + to.x() + ", " + to.y() + ", " + to.z();
        return "PtaOffset{" + near + span + " (" + frame.serialized() + ")}";
    }
}
