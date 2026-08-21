package com.drimoz.punchthemall.core.model.records;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Where an offset actually lands.
 *
 * <p>This is the part of the feature a pack author gets wrong first, and the part that costs the
 * most to debug in game, so every frame is pinned down here rather than discovered by clicking.</p>
 */
class PtaOffsetTest {

    private static final BlockPos ORIGIN = new BlockPos(10, 64, 10);

    private static PtaOffset offset(int x, int y, int z, PtaOffset.Frame frame) {
        return new PtaOffset(x, y, z, frame);
    }

    @Nested
    @DisplayName("the offset itself")
    class Shape {

        @Test
        @DisplayName("NONE is the block that was interacted with")
        void none() {
            assertTrue(PtaOffset.NONE.isZero());
            assertEquals(ORIGIN, PtaOffset.NONE.resolve(ORIGIN, Direction.UP, Direction.NORTH));
        }

        @Test
        @DisplayName("reach is the longest single axis, so the cap bounds a cube")
        void reach() {
            assertEquals(0, PtaOffset.NONE.reach());
            assertEquals(3, offset(1, -3, 2, PtaOffset.Frame.WORLD).reach());
            assertEquals(5, offset(-5, 0, 0, PtaOffset.Frame.WORLD).reach());
        }

        @Test
        @DisplayName("a non-zero offset reports itself as one")
        void nonZero() {
            assertFalse(offset(0, 1, 0, PtaOffset.Frame.WORLD).isZero());
        }
    }

    @Nested
    @DisplayName("world frame")
    class World {

        @Test
        @DisplayName("x is east, y is up, z is south, whoever is clicking")
        void axes() {
            assertEquals(new BlockPos(11, 64, 10), offset(1, 0, 0, PtaOffset.Frame.WORLD).resolve(ORIGIN, Direction.UP, Direction.NORTH));
            assertEquals(new BlockPos(10, 65, 10), offset(0, 1, 0, PtaOffset.Frame.WORLD).resolve(ORIGIN, Direction.UP, Direction.NORTH));
            assertEquals(new BlockPos(10, 64, 11), offset(0, 0, 1, PtaOffset.Frame.WORLD).resolve(ORIGIN, Direction.UP, Direction.NORTH));
        }

        @Test
        @DisplayName("the player's facing does not move it")
        void ignoresPlayer() {
            PtaOffset at = offset(1, 0, -2, PtaOffset.Frame.WORLD);

            BlockPos facingNorth = at.resolve(ORIGIN, Direction.UP, Direction.NORTH);
            BlockPos facingEast = at.resolve(ORIGIN, Direction.UP, Direction.EAST);

            assertEquals(facingNorth, facingEast);
            assertEquals(new BlockPos(11, 64, 8), facingNorth);
        }
    }

    @Nested
    @DisplayName("player frame")
    class PlayerFrame {

        @Test
        @DisplayName("z is forward and follows the player around")
        void forwardTurns() {
            PtaOffset forward = offset(0, 0, 1, PtaOffset.Frame.PLAYER);

            // North is -Z, east is +X: one block "in front of me" tracks the facing.
            assertEquals(new BlockPos(10, 64, 9), forward.resolve(ORIGIN, null, Direction.NORTH));
            assertEquals(new BlockPos(11, 64, 10), forward.resolve(ORIGIN, null, Direction.EAST));
            assertEquals(new BlockPos(10, 64, 11), forward.resolve(ORIGIN, null, Direction.SOUTH));
            assertEquals(new BlockPos(9, 64, 10), forward.resolve(ORIGIN, null, Direction.WEST));
        }

        @Test
        @DisplayName("x is the player's right")
        void right() {
            PtaOffset right = offset(1, 0, 0, PtaOffset.Frame.PLAYER);

            // Facing north (-Z), the player's right hand points east (+X).
            assertEquals(new BlockPos(11, 64, 10), right.resolve(ORIGIN, null, Direction.NORTH));
            assertEquals(new BlockPos(10, 64, 11), right.resolve(ORIGIN, null, Direction.EAST));
        }

        @Test
        @DisplayName("y stays world up, so looking at your feet does not tip the frame over")
        void upIsAlwaysUp() {
            PtaOffset up = offset(0, 2, 0, PtaOffset.Frame.PLAYER);

            assertEquals(new BlockPos(10, 66, 10), up.resolve(ORIGIN, null, Direction.NORTH));
            assertEquals(new BlockPos(10, 66, 10), up.resolve(ORIGIN, null, Direction.WEST));
        }

        @Test
        @DisplayName("a vertical facing falls back to north rather than collapsing the frame")
        void verticalFacingFallsBack() {
            PtaOffset forward = offset(0, 0, 1, PtaOffset.Frame.PLAYER);

            assertEquals(forward.resolve(ORIGIN, null, Direction.NORTH), forward.resolve(ORIGIN, null, Direction.UP));
            assertEquals(forward.resolve(ORIGIN, null, Direction.NORTH), forward.resolve(ORIGIN, null, null));
        }
    }

    @Nested
    @DisplayName("face frame")
    class FaceFrame {

        @Test
        @DisplayName("z is out of the clicked face, where a placed block would go")
        void outOfTheFace() {
            PtaOffset out = offset(0, 0, 1, PtaOffset.Frame.FACE);

            assertEquals(new BlockPos(10, 65, 10), out.resolve(ORIGIN, Direction.UP, Direction.NORTH));
            assertEquals(new BlockPos(10, 63, 10), out.resolve(ORIGIN, Direction.DOWN, Direction.NORTH));
            assertEquals(new BlockPos(10, 64, 9), out.resolve(ORIGIN, Direction.NORTH, Direction.SOUTH));
        }

        @Test
        @DisplayName("right and up stay on the player, so a floor click still has a sane left and right")
        void sidewaysComesFromThePlayer() {
            PtaOffset outAndRight = offset(1, 0, 1, PtaOffset.Frame.FACE);

            assertEquals(new BlockPos(11, 65, 10), outAndRight.resolve(ORIGIN, Direction.UP, Direction.NORTH));
            assertEquals(new BlockPos(10, 65, 11), outAndRight.resolve(ORIGIN, Direction.UP, Direction.EAST));
        }

        @Test
        @DisplayName("with no face to work from it behaves like the player frame")
        void withoutAFace() {
            PtaOffset out = offset(0, 0, 1, PtaOffset.Frame.FACE);

            assertEquals(new BlockPos(10, 64, 9), out.resolve(ORIGIN, null, Direction.NORTH));
        }
    }

    @Nested
    @DisplayName("frame names")
    class Frames {

        @Test
        @DisplayName("parsing accepts any case and rejects anything else")
        void parsing() {
            assertEquals(PtaOffset.Frame.WORLD, PtaOffset.Frame.fromString("world"));
            assertEquals(PtaOffset.Frame.PLAYER, PtaOffset.Frame.fromString("PLAYER"));
            assertEquals(PtaOffset.Frame.FACE, PtaOffset.Frame.fromString("Face"));

            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                    () -> PtaOffset.Frame.fromString("sideways"));
        }
    }

    @Nested
    @DisplayName("regions")
    class Regions {

        private static PtaOffset box(int x1, int y1, int z1, int x2, int y2, int z2, PtaOffset.Frame frame) {
            return new PtaOffset(x1, y1, z1, frame, new PtaOffset(x2, y2, z2, frame));
        }

        @Test
        @DisplayName("a box covers both corners and everything between")
        void covers() {
            PtaOffset area = box(-1, 0, -1, 1, 0, 1, PtaOffset.Frame.WORLD);

            assertTrue(area.isRegion());
            assertEquals(9, area.size());
            assertEquals(9, area.resolveAll(ORIGIN, Direction.UP, Direction.NORTH).size());
            assertTrue(area.resolveAll(ORIGIN, Direction.UP, Direction.NORTH).contains(ORIGIN));
            assertTrue(area.resolveAll(ORIGIN, Direction.UP, Direction.NORTH).contains(new BlockPos(11, 64, 11)));
        }

        @Test
        @DisplayName("corners in either order describe the same box")
        void cornerOrderDoesNotMatter() {
            assertEquals(
                    box(-1, 0, -1, 1, 0, 1, PtaOffset.Frame.WORLD).resolveAll(ORIGIN, Direction.UP, Direction.NORTH),
                    box(1, 0, 1, -1, 0, -1, PtaOffset.Frame.WORLD).resolveAll(ORIGIN, Direction.UP, Direction.NORTH));
        }

        @Test
        @DisplayName("reach is the furthest corner, so the config cap bounds the whole box")
        void reachTakesTheFurthestCorner() {
            assertEquals(5, box(0, 0, 0, 5, 0, 0, PtaOffset.Frame.WORLD).reach());
            assertEquals(4, box(-4, 0, 0, 1, 0, 0, PtaOffset.Frame.WORLD).reach());
        }

        @Test
        @DisplayName("a box is never zero, even when both corners are the origin")
        void neverZero() {
            assertFalse(box(0, 0, 0, 0, 0, 0, PtaOffset.Frame.WORLD).isZero());
            assertEquals(1, box(0, 0, 0, 0, 0, 0, PtaOffset.Frame.WORLD).size());
        }

        @Test
        @DisplayName("the far corner borrows the near corner's frame rather than keeping its own")
        void farCornerBorrowsTheFrame() {
            PtaOffset area = new PtaOffset(0, 0, 0, PtaOffset.Frame.PLAYER,
                    new PtaOffset(1, 0, 0, PtaOffset.Frame.WORLD));

            assertEquals(PtaOffset.Frame.PLAYER, area.to().frame());
        }

        @Test
        @DisplayName("a box in the player frame turns with the player, as a whole")
        void rotatesTogether() {
            PtaOffset area = box(0, 0, 1, 0, 0, 2, PtaOffset.Frame.PLAYER);

            // Facing north (-Z), "one to two blocks in front" is north of the origin.
            assertEquals(
                    java.util.List.of(new BlockPos(10, 64, 9), new BlockPos(10, 64, 8)),
                    area.resolveAll(ORIGIN, null, Direction.NORTH));
            // Facing east (+X), the same box lies east instead.
            assertEquals(
                    java.util.List.of(new BlockPos(11, 64, 10), new BlockPos(12, 64, 10)),
                    area.resolveAll(ORIGIN, null, Direction.EAST));
        }

        @Test
        @DisplayName("a box cannot nest another box")
        void noNesting() {
            PtaOffset inner = box(0, 0, 0, 1, 0, 0, PtaOffset.Frame.WORLD);

            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                    () -> new PtaOffset(0, 0, 0, PtaOffset.Frame.WORLD, inner));
        }
    }
}
