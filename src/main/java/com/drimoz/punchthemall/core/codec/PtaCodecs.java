package com.drimoz.punchthemall.core.codec;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;

import java.util.List;

/**
 * Shared, reusable {@link Codec}s for the schema_version 2 interaction format. Structural only:
 * they validate shape and produce raw specs; resolving ids/tags happens in
 * {@code InteractionSpecResolver}. Pure Mojang-serialization — unchanged across loaders.
 */
public final class PtaCodecs {

    private PtaCodecs() {}

    /** A selector accepting either a single string or a list of strings ('#' prefix = tag). */
    public static final Codec<List<String>> STRING_OR_LIST =
            Codec.either(Codec.STRING, Codec.STRING.listOf()).xmap(
                    either -> either.map(List::of, list -> list),
                    list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list)
            );

    /**
     * A field accepting either a single object or a list of them, decoding to a list either way.
     *
     * <p>Encoding sends a one-element list back as a bare object, so a file that was authored in the
     * short form survives a decode/encode round trip unchanged — which the interaction sync relies
     * on, since it re-encodes every spec to NBT on its way to the client.</p>
     */
    public static <T> Codec<List<T>> objectOrList(Codec<T> codec) {
        return Codec.either(codec, codec.listOf()).xmap(
                either -> either.map(List::of, list -> list),
                list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list)
        );
    }

    /**
     * How a broken block drops, written either as a boolean or by name.
     *
     * <p>{@code true} and {@code false} were the whole vocabulary before tool-aware drops
     * existed, so they keep working and keep their meaning; {@code "tool"} is the new one. A
     * boolean comes back out as a boolean, so old files round-trip unchanged.</p>
     */
    public static final Codec<String> DROP_MODE =
            Codec.either(Codec.BOOL, Codec.STRING).xmap(
                    either -> either.map(drops -> drops ? "vanilla" : "none", mode -> mode),
                    mode -> "vanilla".equalsIgnoreCase(mode) ? Either.left(true)
                            : "none".equalsIgnoreCase(mode) ? Either.left(false)
                            : Either.right(mode)
            );

    /** A scalar coerced to a String (accepts string, boolean or number). */
    public static final Codec<String> SCALAR_STRING =
            Codec.either(Codec.STRING, Codec.either(Codec.BOOL, Codec.LONG)).xmap(
                    either -> either.map(
                            s -> s,
                            boolOrLong -> boolOrLong.map(String::valueOf, String::valueOf)
                    ),
                    Either::left
            );

    /** NBT written as an explicit SNBT string, parsed to a {@link CompoundTag}. */
    public static final Codec<CompoundTag> SNBT = Codec.STRING.comapFlatMap(
            string -> {
                try {
                    return DataResult.success(TagParser.parseTag(string));
                } catch (CommandSyntaxException e) {
                    return DataResult.error(() -> "Invalid SNBT string \"" + string + "\": " + e.getMessage());
                }
            },
            CompoundTag::toString
    );
}
