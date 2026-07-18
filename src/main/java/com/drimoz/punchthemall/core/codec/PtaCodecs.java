package com.drimoz.punchthemall.core.codec;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;

import java.util.List;

/**
 * Shared, reusable {@link Codec}s for the schema_version 2 interaction format.
 *
 * <p>These are intentionally structural only: they validate shape and produce raw specs
 * (strings, ranges, SNBT compounds). Resolving ids/tags into concrete registry objects
 * happens later, in {@code InteractionSpecResolver}, so the runtime model is unchanged.</p>
 */
public final class PtaCodecs {

    private PtaCodecs() {}

    /**
     * A selector accepting either a single string or a list of strings (vanilla "ingredient" feel).
     * A leading '#' marks a tag; otherwise the value is a plain registry id. Normalised to a list.
     */
    public static final Codec<List<String>> STRING_OR_LIST =
            Codec.either(Codec.STRING, Codec.STRING.listOf()).xmap(
                    either -> either.map(List::of, list -> list),
                    list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list)
            );

    /**
     * A scalar coerced to a String. Accepts a JSON string, boolean or number so block-state values
     * can be written naturally ({@code "lit": true} or {@code "lit": "true"}). Encoded as a String.
     */
    public static final Codec<String> SCALAR_STRING =
            Codec.either(Codec.STRING, Codec.either(Codec.BOOL, Codec.LONG)).xmap(
                    either -> either.map(
                            s -> s,
                            boolOrLong -> boolOrLong.map(String::valueOf, String::valueOf)
                    ),
                    Either::left
            );

    /**
     * NBT written as an explicit SNBT string ({@code "{Damage:0}"}) rather than pseudo-JSON.
     * This keeps interaction files strictly valid JSON while parsing to a real {@link CompoundTag}.
     */
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
