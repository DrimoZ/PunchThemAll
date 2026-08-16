package com.drimoz.punchthemall.gametest;

import com.drimoz.punchthemall.PunchThemAll;
import com.drimoz.punchthemall.core.event.TransformationApplier;
import com.drimoz.punchthemall.core.model.classes.PtaBlock;
import com.drimoz.punchthemall.core.model.classes.PtaTransformation;
import com.drimoz.punchthemall.core.model.enums.PtaTransformOp;
import com.drimoz.punchthemall.core.model.records.PtaOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The transformation pipeline, running in a real world.
 *
 * <p>{@code TransformationApplier} is the one part of the feature no unit test can reach: every
 * decision it makes is a question about a {@code Level} — is this chunk loaded, can this block be
 * replaced, would this block survive here. These run on a live server with no client, so they can be
 * driven from the command line like any other test.</p>
 *
 * <p>All of them share one small flat template and drive the applier directly rather than simulating
 * clicks. The click path is the handler's business; what needs watching here is what actually
 * happens to the blocks.</p>
 */
@GameTestHolder(PunchThemAll.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TransformationGameTests {

    private static final String PLATFORM = "pta_platform";
    private static final String BATCH = "transformations";

    /** Sits on the template floor, with room around it in every direction. */
    private static final BlockPos ORIGIN = new BlockPos(2, 1, 2);

    private TransformationGameTests() {}

    // Helpers

    private static PtaTransformation replace(Block into, PtaOffset at) {
        return PtaTransformation.createBlock(1.0, into, null, null, null, null)
                .withPlacement(PtaTransformOp.REPLACE, at, null, true);
    }

    private static PtaTransformation place(Block into, PtaOffset at, PtaBlock require) {
        return PtaTransformation.createBlock(1.0, into, null, null, null, null)
                .withPlacement(PtaTransformOp.PLACE, at, require, true);
    }

    private static PtaTransformation breaking(PtaOffset at, boolean drops) {
        return PtaTransformation.createAir(1.0, null, null)
                .withPlacement(PtaTransformOp.BREAK, at, null, drops);
    }

    private static PtaOffset at(int x, int y, int z) {
        return new PtaOffset(x, y, z, PtaOffset.Frame.WORLD);
    }

    /** Runs the applier against the helper's world, translating the relative origin for it. */
    private static List<BlockPos> apply(GameTestHelper helper, List<PtaTransformation> transformations) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        return TransformationApplier.apply(
                helper.getLevel(), player,
                helper.absolutePos(ORIGIN), Direction.UP,
                transformations, helper.getLevel().getRandom(), new HashSet<>()
        );
    }

    // replace

    @GameTest(template = PLATFORM, batch = BATCH)
    public static void replaceActsOnTheBlockItself(GameTestHelper helper) {
        helper.setBlock(ORIGIN, Blocks.STONE);

        apply(helper, List.of(replace(Blocks.COBBLESTONE, PtaOffset.NONE)));

        helper.assertBlockPresent(Blocks.COBBLESTONE, ORIGIN);
        helper.succeed();
    }

    @GameTest(template = PLATFORM, batch = BATCH)
    public static void replaceReachesAnOffsetBlock(GameTestHelper helper) {
        helper.setBlock(ORIGIN, Blocks.STONE);
        helper.setBlock(ORIGIN.above(), Blocks.DIRT);

        apply(helper, List.of(replace(Blocks.COBBLESTONE, at(0, 1, 0))));

        // The clicked block is untouched; the one above it changed.
        helper.assertBlockPresent(Blocks.STONE, ORIGIN);
        helper.assertBlockPresent(Blocks.COBBLESTONE, ORIGIN.above());
        helper.succeed();
    }

    @GameTest(template = PLATFORM, batch = BATCH)
    public static void replaceOverwritesWhateverIsThere(GameTestHelper helper) {
        helper.setBlock(ORIGIN, Blocks.STONE);
        helper.setBlock(ORIGIN.north(), Blocks.OAK_LOG);

        apply(helper, List.of(replace(Blocks.COBBLESTONE, at(0, 0, -1))));

        helper.assertBlockPresent(Blocks.COBBLESTONE, ORIGIN.north());
        helper.succeed();
    }

    // break

    @GameTest(template = PLATFORM, batch = BATCH)
    public static void breakRemovesAnOffsetBlockAndDropsIt(GameTestHelper helper) {
        helper.setBlock(ORIGIN, Blocks.STONE);
        helper.setBlock(ORIGIN.above(), Blocks.OAK_LOG);

        apply(helper, List.of(breaking(at(0, 1, 0), true)));

        helper.assertBlockPresent(Blocks.STONE, ORIGIN);
        helper.assertBlockNotPresent(Blocks.OAK_LOG, ORIGIN.above());
        helper.assertItemEntityPresent(Blocks.OAK_LOG.asItem(), ORIGIN.above(), 2.0);
        helper.succeed();
    }

    @GameTest(template = PLATFORM, batch = BATCH)
    public static void breakWithoutDropsLeavesNothingBehind(GameTestHelper helper) {
        helper.setBlock(ORIGIN, Blocks.STONE);
        helper.setBlock(ORIGIN.above(), Blocks.OAK_LOG);

        apply(helper, List.of(breaking(at(0, 1, 0), false)));

        helper.assertBlockNotPresent(Blocks.OAK_LOG, ORIGIN.above());
        helper.assertItemEntityCountIs(Blocks.OAK_LOG.asItem(), ORIGIN.above(), 2.0, 0);
        helper.succeed();
    }

    @GameTest(template = PLATFORM, batch = BATCH)
    public static void breakingNothingIsSkipped(GameTestHelper helper) {
        helper.setBlock(ORIGIN, Blocks.STONE);
        helper.setBlock(ORIGIN.above(), Blocks.AIR);

        List<BlockPos> applied = apply(helper, List.of(breaking(at(0, 1, 0), true)));

        if (!applied.isEmpty()) helper.fail("breaking air should do nothing");
        helper.succeed();
    }

    // place

    @GameTest(template = PLATFORM, batch = BATCH)
    public static void placeWritesIntoAir(GameTestHelper helper) {
        helper.setBlock(ORIGIN, Blocks.STONE);
        helper.setBlock(ORIGIN.above(), Blocks.AIR);

        apply(helper, List.of(place(Blocks.TORCH, at(0, 1, 0), null)));

        helper.assertBlockPresent(Blocks.TORCH, ORIGIN.above());
        helper.succeed();
    }

    @GameTest(template = PLATFORM, batch = BATCH)
    public static void placeRefusesAnOccupiedDestination(GameTestHelper helper) {
        helper.setBlock(ORIGIN, Blocks.STONE);
        helper.setBlock(ORIGIN.above(), Blocks.OAK_LOG);

        apply(helper, List.of(place(Blocks.TORCH, at(0, 1, 0), null)));

        helper.assertBlockPresent(Blocks.OAK_LOG, ORIGIN.above());
        helper.succeed();
    }

    @GameTest(template = PLATFORM, batch = BATCH)
    public static void placeRefusesWhereTheBlockCouldNotSurvive(GameTestHelper helper) {
        helper.setBlock(ORIGIN, Blocks.STONE);
        // Two blocks up: air below it, so a torch has nothing to stand on. Placing it anyway would
        // pop a tick later and read as a mod bug.
        helper.setBlock(ORIGIN.above(), Blocks.AIR);
        helper.setBlock(ORIGIN.above(2), Blocks.AIR);

        apply(helper, List.of(place(Blocks.TORCH, at(0, 2, 0), null)));

        helper.assertBlockNotPresent(Blocks.TORCH, ORIGIN.above(2));
        helper.succeed();
    }

    // require

    @GameTest(template = PLATFORM, batch = BATCH)
    public static void requireGatesOnTheDestination(GameTestHelper helper) {
        helper.setBlock(ORIGIN, Blocks.STONE);
        helper.setBlock(ORIGIN.above(), Blocks.DIRT);

        PtaBlock mustBeSand = PtaBlock.createBlock(Set.of(Blocks.SAND), null, null, null, null);
        apply(helper, List.of(place(Blocks.TORCH, at(0, 1, 0), mustBeSand)));

        helper.assertBlockPresent(Blocks.DIRT, ORIGIN.above());
        helper.succeed();
    }

    @GameTest(template = PLATFORM, batch = BATCH)
    public static void requireLetsAMatchingDestinationThrough(GameTestHelper helper) {
        helper.setBlock(ORIGIN, Blocks.STONE);
        helper.setBlock(ORIGIN.above(), Blocks.SAND);

        PtaBlock mustBeSand = PtaBlock.createBlock(Set.of(Blocks.SAND), null, null, null, null);
        PtaTransformation transformation = PtaTransformation.createBlock(1.0, Blocks.GLASS, null, null, null, null)
                .withPlacement(PtaTransformOp.REPLACE, at(0, 1, 0), mustBeSand, true);

        apply(helper, List.of(transformation));

        helper.assertBlockPresent(Blocks.GLASS, ORIGIN.above());
        helper.succeed();
    }

    // lists

    @GameTest(template = PLATFORM, batch = BATCH)
    public static void aListAppliesEveryEntry(GameTestHelper helper) {
        helper.setBlock(ORIGIN, Blocks.STONE);
        helper.setBlock(ORIGIN.above(), Blocks.AIR);
        helper.setBlock(ORIGIN.north(), Blocks.AIR);

        List<BlockPos> applied = apply(helper, List.of(
                replace(Blocks.GLASS, at(0, 1, 0)),
                replace(Blocks.GLASS, at(0, 0, -1))
        ));

        helper.assertBlockPresent(Blocks.GLASS, ORIGIN.above());
        helper.assertBlockPresent(Blocks.GLASS, ORIGIN.north());
        if (applied.size() != 2) helper.fail("expected both entries to apply, got " + applied.size());
        helper.succeed();
    }

    @GameTest(template = PLATFORM, batch = BATCH)
    public static void twoEntriesOnOneBlockOnlyApplyOnce(GameTestHelper helper) {
        helper.setBlock(ORIGIN, Blocks.STONE);
        helper.setBlock(ORIGIN.above(), Blocks.AIR);

        // Both aim at the same block. The second is planned against a world the first has already
        // changed, so it must be skipped rather than layered on top.
        List<BlockPos> applied = apply(helper, List.of(
                replace(Blocks.GLASS, at(0, 1, 0)),
                replace(Blocks.OAK_LOG, at(0, 1, 0))
        ));

        helper.assertBlockPresent(Blocks.GLASS, ORIGIN.above());
        if (applied.size() != 1) helper.fail("expected one applied transformation, got " + applied.size());
        helper.succeed();
    }

    @GameTest(template = PLATFORM, batch = BATCH)
    public static void aSecondInteractionCannotReuseATouchedBlock(GameTestHelper helper) {
        helper.setBlock(ORIGIN, Blocks.STONE);

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Set<BlockPos> touched = new HashSet<>();

        List<BlockPos> first = TransformationApplier.apply(helper.getLevel(), player,
                helper.absolutePos(ORIGIN), Direction.UP,
                List.of(replace(Blocks.GLASS, PtaOffset.NONE)), helper.getLevel().getRandom(), touched);
        touched.addAll(first);

        List<BlockPos> second = TransformationApplier.apply(helper.getLevel(), player,
                helper.absolutePos(ORIGIN), Direction.UP,
                List.of(replace(Blocks.OAK_LOG, PtaOffset.NONE)), helper.getLevel().getRandom(), touched);

        helper.assertBlockPresent(Blocks.GLASS, ORIGIN);
        if (!second.isEmpty()) helper.fail("a block already changed on this click must not change again");
        helper.succeed();
    }

    // limits

    @GameTest(template = PLATFORM, batch = BATCH)
    public static void anOffsetPastTheConfiguredCapIsSkipped(GameTestHelper helper) {
        helper.setBlock(ORIGIN, Blocks.STONE);

        // The default cap is 8; this asks for far more and must be refused outright.
        List<BlockPos> applied = apply(helper, List.of(replace(Blocks.GLASS, at(0, 64, 0))));

        if (!applied.isEmpty()) helper.fail("an offset past max_transformation_offset must not apply");
        helper.succeed();
    }

    @GameTest(template = PLATFORM, batch = BATCH)
    public static void positionsAreReportedBackToTheCaller(GameTestHelper helper) {
        helper.setBlock(ORIGIN, Blocks.STONE);
        helper.setBlock(ORIGIN.above(), Blocks.AIR);

        List<BlockPos> applied = apply(helper, List.of(replace(Blocks.GLASS, at(0, 1, 0))));

        // Compared in absolute coordinates on purpose. The test area can be rotated, and
        // relativePos does not undo that rotation, so round-tripping through it would compare two
        // different frames and fail on a perfectly correct result.
        BlockPos expected = helper.absolutePos(ORIGIN).above();
        if (!applied.contains(expected)) {
            helper.fail("the applier must report the block it changed; expected " + expected + ", got " + applied);
        }
        helper.succeed();
    }
}
