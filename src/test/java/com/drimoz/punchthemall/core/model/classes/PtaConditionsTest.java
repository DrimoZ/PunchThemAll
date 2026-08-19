package com.drimoz.punchthemall.core.model.classes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Environmental and player gating. A {@code Level} is mocked rather than built: constructing a real
 * one drags in a whole server, and every branch here reads exactly one accessor off it.
 */
class PtaConditionsTest {

    private static final BlockPos POS = new BlockPos(10, 64, -30);

    private final Level level = mock(Level.class);
    private final Player player = mock(Player.class);
    private final FoodData foodData = mock(FoodData.class);

    PtaConditionsTest() {
        lenient().when(level.getDayTime()).thenReturn(1000L);       // day
        lenient().when(level.isRaining()).thenReturn(false);
        lenient().when(level.isThundering()).thenReturn(false);
        lenient().when(level.getMaxLocalRawBrightness(POS)).thenReturn(15);
        lenient().when(player.isShiftKeyDown()).thenReturn(false);
        lenient().when(player.getFoodData()).thenReturn(foodData);
        lenient().when(foodData.getFoodLevel()).thenReturn(20);
    }

    private static PtaConditions with(
            PtaConditions.Time time, Set<PtaConditions.Weather> weather,
            Integer yMin, Integer yMax, Integer lightMin, Integer lightMax,
            Boolean sneaking, int minFood, int minXp
    ) {
        return new PtaConditions(time, weather, yMin, yMax, lightMin, lightMax, sneaking, minFood, minXp, java.util.List.of());
    }

    private static PtaConditions time(PtaConditions.Time time) {
        return with(time, Set.of(), null, null, null, null, null, 0, 0);
    }

    @Test
    @DisplayName("EMPTY is empty and always matches")
    void emptyMatchesEverything() {
        assertTrue(PtaConditions.EMPTY.isEmpty());
        assertTrue(PtaConditions.EMPTY.matches(level, player, POS));
    }

    @Test
    @DisplayName("any single set field makes it non-empty")
    void nonEmptyDetection() {
        assertFalse(time(PtaConditions.Time.DAY).isEmpty());
        assertFalse(with(PtaConditions.Time.ANY, Set.of(PtaConditions.Weather.RAIN), null, null, null, null, null, 0, 0).isEmpty());
        assertFalse(with(PtaConditions.Time.ANY, Set.of(), 0, null, null, null, null, 0, 0).isEmpty());
        assertFalse(with(PtaConditions.Time.ANY, Set.of(), null, null, 4, null, null, 0, 0).isEmpty());
        assertFalse(with(PtaConditions.Time.ANY, Set.of(), null, null, null, null, true, 0, 0).isEmpty());
        assertFalse(with(PtaConditions.Time.ANY, Set.of(), null, null, null, null, null, 6, 0).isEmpty());
        assertFalse(with(PtaConditions.Time.ANY, Set.of(), null, null, null, null, null, 0, 3).isEmpty());
    }

    @Test
    @DisplayName("day and night split at 12000 ticks")
    void dayNight() {
        when(level.getDayTime()).thenReturn(1000L);
        assertTrue(time(PtaConditions.Time.DAY).matches(level, player, POS));
        assertFalse(time(PtaConditions.Time.NIGHT).matches(level, player, POS));

        when(level.getDayTime()).thenReturn(13000L);
        assertFalse(time(PtaConditions.Time.DAY).matches(level, player, POS));
        assertTrue(time(PtaConditions.Time.NIGHT).matches(level, player, POS));
    }

    @Test
    @DisplayName("the day/night test uses the time of day, not the total elapsed time")
    void dayTimeWrapsPerDay() {
        // Day 40, mid-morning. Without the modulo this would read as night forever after day 1.
        when(level.getDayTime()).thenReturn(24000L * 40 + 1000L);
        assertTrue(time(PtaConditions.Time.DAY).matches(level, player, POS));
    }

    @Test
    @DisplayName("weather matches the current state, and thunder is not rain")
    void weather() {
        PtaConditions rainOnly = with(PtaConditions.Time.ANY, Set.of(PtaConditions.Weather.RAIN),
                null, null, null, null, null, 0, 0);
        PtaConditions clearOnly = with(PtaConditions.Time.ANY, Set.of(PtaConditions.Weather.CLEAR),
                null, null, null, null, null, 0, 0);
        PtaConditions thunderOnly = with(PtaConditions.Time.ANY, Set.of(PtaConditions.Weather.THUNDER),
                null, null, null, null, null, 0, 0);

        assertTrue(clearOnly.matches(level, player, POS));
        assertFalse(rainOnly.matches(level, player, POS));

        when(level.isRaining()).thenReturn(true);
        assertTrue(rainOnly.matches(level, player, POS));
        assertFalse(clearOnly.matches(level, player, POS));

        when(level.isThundering()).thenReturn(true);
        assertTrue(thunderOnly.matches(level, player, POS));
        assertFalse(rainOnly.matches(level, player, POS), "thunder should not also count as rain");
    }

    @Test
    @DisplayName("several weathers are alternatives")
    void weatherIsAnyOf() {
        PtaConditions wet = with(PtaConditions.Time.ANY,
                Set.of(PtaConditions.Weather.RAIN, PtaConditions.Weather.THUNDER),
                null, null, null, null, null, 0, 0);

        assertFalse(wet.matches(level, player, POS));
        when(level.isRaining()).thenReturn(true);
        assertTrue(wet.matches(level, player, POS));
    }

    @Test
    @DisplayName("the Y range is inclusive on both ends")
    void yRange() {
        assertTrue(with(PtaConditions.Time.ANY, Set.of(), 64, 64, null, null, null, 0, 0).matches(level, player, POS));
        assertTrue(with(PtaConditions.Time.ANY, Set.of(), 0, 128, null, null, null, 0, 0).matches(level, player, POS));
        assertFalse(with(PtaConditions.Time.ANY, Set.of(), 65, null, null, null, null, 0, 0).matches(level, player, POS));
        assertFalse(with(PtaConditions.Time.ANY, Set.of(), null, 63, null, null, null, 0, 0).matches(level, player, POS));
    }

    @Test
    @DisplayName("the light range is inclusive and only read when set")
    void lightRange() {
        when(level.getMaxLocalRawBrightness(POS)).thenReturn(7);

        assertTrue(with(PtaConditions.Time.ANY, Set.of(), null, null, 0, 7, null, 0, 0).matches(level, player, POS));
        assertTrue(with(PtaConditions.Time.ANY, Set.of(), null, null, 7, null, null, 0, 0).matches(level, player, POS));
        assertFalse(with(PtaConditions.Time.ANY, Set.of(), null, null, 8, null, null, 0, 0).matches(level, player, POS));
        assertFalse(with(PtaConditions.Time.ANY, Set.of(), null, null, null, 6, null, 0, 0).matches(level, player, POS));
    }

    @Test
    @DisplayName("requires_sneaking is an equality test, so false means 'must not sneak'")
    void sneaking() {
        PtaConditions mustSneak = with(PtaConditions.Time.ANY, Set.of(), null, null, null, null, true, 0, 0);
        PtaConditions mustNotSneak = with(PtaConditions.Time.ANY, Set.of(), null, null, null, null, false, 0, 0);

        assertFalse(mustSneak.matches(level, player, POS));
        assertTrue(mustNotSneak.matches(level, player, POS));

        when(player.isShiftKeyDown()).thenReturn(true);
        assertTrue(mustSneak.matches(level, player, POS));
        assertFalse(mustNotSneak.matches(level, player, POS));
    }

    @Test
    @DisplayName("min_food gates on the hunger bar")
    void minFood() {
        PtaConditions needsSix = with(PtaConditions.Time.ANY, Set.of(), null, null, null, null, null, 6, 0);

        when(foodData.getFoodLevel()).thenReturn(20);
        assertTrue(needsSix.matches(level, player, POS));

        when(foodData.getFoodLevel()).thenReturn(6);
        assertTrue(needsSix.matches(level, player, POS));

        when(foodData.getFoodLevel()).thenReturn(5);
        assertFalse(needsSix.matches(level, player, POS));
    }

    @Test
    @DisplayName("min_xp_levels gates on experience")
    void minXpLevels() {
        // Player.experienceLevel is a plain field, so a mock reports 0 — enough to prove the gate
        // rejects, which is the branch that decides whether an interaction fires.
        PtaConditions needsLevels = with(PtaConditions.Time.ANY, Set.of(), null, null, null, null, null, 0, 3);
        assertFalse(needsLevels.matches(level, player, POS));

        player.experienceLevel = 3;
        assertTrue(needsLevels.matches(level, player, POS));
    }

    @Test
    @DisplayName("conditions combine with AND: one failure is enough")
    void conditionsAreConjunctive() {
        PtaConditions dayAndSneaking =
                with(PtaConditions.Time.DAY, Set.of(), null, null, null, null, true, 0, 0);

        when(level.getDayTime()).thenReturn(1000L);   // day: passes
        when(player.isShiftKeyDown()).thenReturn(false); // sneaking: fails
        assertFalse(dayAndSneaking.matches(level, player, POS));

        when(player.isShiftKeyDown()).thenReturn(true);
        assertTrue(dayAndSneaking.matches(level, player, POS));
    }
}
