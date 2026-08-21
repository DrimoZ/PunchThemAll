package com.drimoz.punchthemall.core.model.classes;

import com.drimoz.punchthemall.core.model.records.PtaNeighbour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Set;

/**
 * Optional environmental / player gating for an interaction (schema_version 2, §5.7). Every field is
 * optional; {@link #EMPTY} always matches. Biome/dimension gating stays on {@link PtaInteraction}.
 */
public record PtaConditions(
        Time time,
        Set<Weather> weather,
        Integer yMin,
        Integer yMax,
        Integer lightMin,
        Integer lightMax,
        Boolean requiresSneaking,
        int minFood,
        int minXpLevels,
        List<PtaNeighbour> neighbours
) {
    public enum Time { ANY, DAY, NIGHT }
    public enum Weather { CLEAR, RAIN, THUNDER }

    public static final PtaConditions EMPTY =
            new PtaConditions(Time.ANY, Set.of(), null, null, null, null, null, 0, 0, List.of());

    /** The same conditions with the sneak gate removed, once the type carries it instead. */
    public PtaConditions withoutSneaking() {
        if (requiresSneaking == null) return this;
        return new PtaConditions(time, weather, yMin, yMax, lightMin, lightMax, null,
                minFood, minXpLevels, neighbours);
    }

    public boolean isEmpty() {
        return time == Time.ANY && weather.isEmpty() && yMin == null && yMax == null
                && lightMin == null && lightMax == null && requiresSneaking == null
                && minFood <= 0 && minXpLevels <= 0 && neighbours.isEmpty();
    }

    /** Kept for callers with no clicked face to offer; neighbour offsets then fall back to the player frame. */
    public boolean matches(Level level, Player player, BlockPos pos) {
        return matches(level, player, pos, null);
    }

    public boolean matches(Level level, Player player, BlockPos pos, Direction face) {
        if (isEmpty()) return true;

        if (time != Time.ANY) {
            // 26.1 replaced the fixed day-time field with datapack world clocks; getDayTime is gone
            // and getOverworldClockTime is the nearest reading. Same 24000-tick scale, so the
            // arithmetic is unchanged.
            //
            // Not isBrightOutside(): that folds in weather and dimension, which PTA gates separately
            // through `weather`, so a `time: day` interaction would silently stop firing in rain.
            //
            // One real difference: this is explicitly the *overworld* clock, so in the Nether and the
            // End it no longer reads that dimension own time. Vanilla kept them in step, so this
            // should be invisible.
            long dayTime = level.getOverworldClockTime() % 24000L;
            boolean isDay = dayTime < 12000L;
            if (time == Time.DAY && !isDay) return false;
            if (time == Time.NIGHT && isDay) return false;
        }

        if (!weather.isEmpty() && !weather.contains(currentWeather(level))) {
            return false;
        }

        if (yMin != null && pos.getY() < yMin) return false;
        if (yMax != null && pos.getY() > yMax) return false;

        if (lightMin != null || lightMax != null) {
            int light = level.getMaxLocalRawBrightness(pos);
            if (lightMin != null && light < lightMin) return false;
            if (lightMax != null && light > lightMax) return false;
        }

        if (requiresSneaking != null && player.isShiftKeyDown() != requiresSneaking) return false;

        if (minFood > 0 && player.getFoodData().getFoodLevel() < minFood) return false;
        if (minXpLevels > 0 && player.experienceLevel < minXpLevels) return false;

        for (PtaNeighbour neighbour : neighbours) {
            if (!neighbour.matches(level, pos, face, player.getDirection())) return false;
        }

        return true;
    }

    private static Weather currentWeather(Level level) {
        if (level.isThundering()) return Weather.THUNDER;
        if (level.isRaining()) return Weather.RAIN;
        return Weather.CLEAR;
    }
}
