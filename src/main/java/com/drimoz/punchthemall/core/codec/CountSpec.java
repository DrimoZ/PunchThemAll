package com.drimoz.punchthemall.core.codec;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/**
 * A count/range that accepts three JSON shapes, unified into a {@code [min, max]} range:
 * <ul>
 *     <li>a plain integer: {@code 3} -> min=max=3</li>
 *     <li>{@code {"count": 3}} -> min=max=3</li>
 *     <li>{@code {"min": 1, "max": 3}} -> min=1, max=3 (max defaults to min when absent)</li>
 * </ul>
 * The effective floor differs by context (0 for drop pools, 1 for player costs), so it is applied
 * lazily in {@link #resolve(int)} rather than baked into the spec.
 */
public record CountSpec(Optional<Integer> count, Optional<Integer> min, Optional<Integer> max) {

    private static final Codec<CountSpec> OBJECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("count").forGetter(CountSpec::count),
            Codec.INT.optionalFieldOf("min").forGetter(CountSpec::min),
            Codec.INT.optionalFieldOf("max").forGetter(CountSpec::max)
    ).apply(instance, CountSpec::new));

    public static final Codec<CountSpec> CODEC = Codec.either(Codec.INT, OBJECT_CODEC).xmap(
            either -> either.map(CountSpec::exact, object -> object),
            spec -> spec.isExact() ? Either.left(spec.count.orElseGet(() -> spec.min.orElse(0))) : Either.right(spec)
    );

    public static CountSpec exact(int value) {
        return new CountSpec(Optional.of(value), Optional.empty(), Optional.empty());
    }

    private boolean isExact() {
        return count.isPresent() || (min.isPresent() && max.isEmpty());
    }

    /**
     * @param defaultMin floor applied to both bounds (0 for pools, 1 for costs)
     * @return a resolved [min, max] range with min &lt;= max
     */
    public Range resolve(int defaultMin) {
        if (count.isPresent()) {
            int c = Math.max(defaultMin, count.get());
            return new Range(c, c);
        }
        int lo = Math.max(defaultMin, min.orElse(defaultMin));
        int hi = Math.max(lo, max.orElse(lo));
        return new Range(lo, hi);
    }

    public record Range(int min, int max) {}
}
