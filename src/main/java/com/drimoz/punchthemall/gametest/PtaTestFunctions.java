package com.drimoz.punchthemall.gametest;

import com.drimoz.punchthemall.PunchThemAll;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The in-world test bodies, addressed by name.
 *
 * <p>26.1 wants a test body in {@code Registries.TEST_FUNCTION}, which is built and frozen during
 * {@code BuiltInRegistries} class initialisation — before any mod exists. So the bodies live here
 * instead, and {@link PtaTestInstance} looks one up when it runs. The map is the registry PTA is
 * allowed to have.</p>
 */
public final class PtaTestFunctions {

    private static final Map<Identifier, Consumer<GameTestHelper>> FUNCTIONS = new LinkedHashMap<>();

    static {
        register("replace_acts_on_the_block_itself", TransformationGameTests::replaceActsOnTheBlockItself);
        register("replace_reaches_an_offset_block", TransformationGameTests::replaceReachesAnOffsetBlock);
        register("replace_overwrites_whatever_is_there", TransformationGameTests::replaceOverwritesWhateverIsThere);
        register("break_removes_an_offset_block_and_drops_it", TransformationGameTests::breakRemovesAnOffsetBlockAndDropsIt);
        register("break_without_drops_leaves_nothing_behind", TransformationGameTests::breakWithoutDropsLeavesNothingBehind);
        register("breaking_nothing_is_skipped", TransformationGameTests::breakingNothingIsSkipped);
        register("place_writes_into_air", TransformationGameTests::placeWritesIntoAir);
        register("place_refuses_an_occupied_destination", TransformationGameTests::placeRefusesAnOccupiedDestination);
        register("place_refuses_where_the_block_could_not_survive", TransformationGameTests::placeRefusesWhereTheBlockCouldNotSurvive);
        register("require_gates_on_the_destination", TransformationGameTests::requireGatesOnTheDestination);
        register("require_lets_a_matching_destination_through", TransformationGameTests::requireLetsAMatchingDestinationThrough);
        register("a_list_applies_every_entry", TransformationGameTests::aListAppliesEveryEntry);
        register("two_entries_on_one_block_only_apply_once", TransformationGameTests::twoEntriesOnOneBlockOnlyApplyOnce);
        register("a_second_interaction_cannot_reuse_a_touched_block", TransformationGameTests::aSecondInteractionCannotReuseATouchedBlock);
        register("an_offset_past_the_configured_cap_is_skipped", TransformationGameTests::anOffsetPastTheConfiguredCapIsSkipped);
        register("positions_are_reported_back_to_the_caller", TransformationGameTests::positionsAreReportedBackToTheCaller);
        register("a_region_covers_every_block_between_its_corners", TransformationGameTests::aRegionCoversEveryBlockBetweenItsCorners);
        register("a_region_skips_only_the_blocks_it_may_not_touch", TransformationGameTests::aRegionSkipsOnlyTheBlocksItMayNotTouch);
        register("copy_writes_the_block_found_at_its_source", TransformationGameTests::copyWritesTheBlockFoundAtItsSource);
        register("copy_paired_with_a_break_moves_the_block", TransformationGameTests::copyPairedWithABreakMovesTheBlock);
        register("break_with_tool_drops_honours_silk_touch", TransformationGameTests::breakWithToolDropsHonoursSilkTouch);
        register("break_with_vanilla_drops_ignores_the_tool", TransformationGameTests::breakWithVanillaDropsIgnoresTheTool);
        register("a_neighbour_condition_reads_the_block_it_points_at", TransformationGameTests::aNeighbourConditionReadsTheBlockItPointsAt);
        register("an_inverted_neighbour_condition_is_the_other_way_round", TransformationGameTests::anInvertedNeighbourConditionIsTheOtherWayRound);
        register("a_vetoed_break_leaves_the_block_alone", TransformationGameTests::aVetoedBreakLeavesTheBlockAlone);
        register("a_vetoed_place_writes_nothing", TransformationGameTests::aVetoedPlaceWritesNothing);
        register("a_veto_only_covers_the_block_it_protects", TransformationGameTests::aVetoOnlyCoversTheBlockItProtects);
        register("a_veto_stops_only_its_own_block_in_a_region", TransformationGameTests::aVetoStopsOnlyItsOwnBlockInARegion);
    }

    private PtaTestFunctions() {}

    private static void register(String name, Consumer<GameTestHelper> body) {
        FUNCTIONS.put(Identifier.fromNamespaceAndPath(PunchThemAll.MOD_ID, name), body);
    }

    public static Map<Identifier, Consumer<GameTestHelper>> all() {
        return Map.copyOf(FUNCTIONS);
    }

    public static Consumer<GameTestHelper> get(Identifier name) {
        Consumer<GameTestHelper> body = FUNCTIONS.get(name);
        if (body == null) throw new IllegalStateException("No PunchThemAll test body named " + name);
        return body;
    }
}
