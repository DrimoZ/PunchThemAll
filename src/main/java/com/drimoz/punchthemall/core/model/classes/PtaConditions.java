package com.drimoz.punchthemall.core.model.classes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Set;

/**
 * Optional environmental / player gating for an interaction (schema_version 2, §5.7).
 *
 * <p>Every field is optional; an {@link #EMPTY} instance always matches, so interactions that do not
 * declare {@code conditions} behave exactly as before. Biome/dimension gating stays on
 * {@link PtaInteraction} itself; this covers time, weather, altitude, light and player state.</p>
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
        int minXpLevels
) {
    public enum Time { ANY, DAY, NIGHT }
    public enum Weather { CLEAR, RAIN, THUNDER }

    public static final PtaConditions EMPTY =
            new PtaConditions(Time.ANY, Set.of(), null, null, null, null, null, 0, 0);

    public boolean isEmpty() {
        return time == Time.ANY && weather.isEmpty() && yMin == null && yMax == null
                && lightMin == null && lightMax == null && requiresSneaking == null
                && minFood <= 0 && minXpLevels <= 0;
    }

    public boolean matches(Level level, Player player, BlockPos pos) {
        if (isEmpty()) return true;

        if (time != Time.ANY) {
            long dayTime = level.getDayTime() % 24000L;
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

        return true;
    }

    private static Weather currentWeather(Level level) {
        if (level.isThundering()) return Weather.THUNDER;
        if (level.isRaining()) return Weather.RAIN;
        return Weather.CLEAR;
    }
}
