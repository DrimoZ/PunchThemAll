package com.drimoz.punchthemall.core.util;

import com.drimoz.punchthemall.core.model.classes.PtaNbtPredicate;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Turns an interaction's item constraints into sentences a player can read.
 *
 * <p>The three authoring forms — the SNBT {@code nbt.whitelist} / {@code nbt.blacklist} and the
 * path-based {@code nbt_predicates} — describe the same idea from the player's side: the held item
 * either qualifies or it does not. So they collapse into two blocks, <b>must have</b> and
 * <b>must not have</b>, instead of leaking the raw tag structure into the tooltip. Both recipe
 * viewers render the result, so the wording stays identical between them.</p>
 */
public final class ItemConstraintDescriber {

    private static final String ENCHANTMENTS = "Enchantments";
    private static final String DAMAGE = "Damage";

    private ItemConstraintDescriber() {}

    /** What the constraints are about, so the headers read naturally. */
    public enum Subject {
        ITEM(TranslationKeys.INTERACTION_NBT_REQUIRES, TranslationKeys.INTERACTION_NBT_FORBIDS),
        TARGET(TranslationKeys.INTERACTION_NBT_TARGET_REQUIRES, TranslationKeys.INTERACTION_NBT_TARGET_FORBIDS);

        private final String requiresKey;
        private final String forbidsKey;

        Subject(String requiresKey, String forbidsKey) {
            this.requiresKey = requiresKey;
            this.forbidsKey = forbidsKey;
        }
    }

    /** All constraint lines for one hand or target, headers included. Empty when unconstrained. */
    public static List<Component> describe(CompoundTag whitelist, CompoundTag blacklist, List<PtaNbtPredicate> predicates, Subject subject) {
        List<Component> required = new ArrayList<>();
        List<Component> forbidden = new ArrayList<>();

        if (whitelist != null) describeCompound(whitelist, required);
        if (blacklist != null) describeCompound(blacklist, forbidden);
        if (predicates != null) {
            for (PtaNbtPredicate predicate : predicates) {
                required.add(describePredicate(predicate));
            }
        }

        if (required.isEmpty() && forbidden.isEmpty()) return List.of();

        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());
        addBlock(lines, subject.requiresKey, required, ChatFormatting.GREEN);
        addBlock(lines, subject.forbidsKey, forbidden, ChatFormatting.RED);
        return lines;
    }

    private static void addBlock(List<Component> lines, String headerKey, List<Component> entries, ChatFormatting colour) {
        if (entries.isEmpty()) return;

        lines.add(Component.translatable(headerKey).withStyle(colour, ChatFormatting.BOLD));
        for (Component entry : entries) {
            lines.add(Component.literal("  • ").withStyle(ChatFormatting.DARK_GRAY).append(entry.copy().setStyle(entry.getStyle().applyTo(net.minecraft.network.chat.Style.EMPTY.withColor(colour)))));
        }
    }

    // Predicates -------------------------------------------------------------------------------

    private static Component describePredicate(PtaNbtPredicate predicate) {
        Optional<String> enchantId = enchantmentIdFrom(predicate.where());

        // "Enchantments[].lvl" with a where-filter on the id is by far the common case, and reads far
        // better as "Efficiency I - V" than as a path plus a raw filter compound.
        if (enchantId.isPresent() && predicate.path().endsWith("lvl")) {
            return Component.literal(enchantmentName(enchantId.get()) + " " + romanRange(predicate.intMin(), predicate.intMax()));
        }

        MutableComponent line = Component.literal(friendlyPath(predicate.path()));
        String range = numericRange(predicate.intMin(), predicate.intMax());
        if (!range.isEmpty()) {
            line.append(Component.literal(" " + range));
        } else {
            line.append(Component.translatable(TranslationKeys.INTERACTION_NBT_PRESENT));
        }
        if (enchantId.isPresent()) {
            line.append(Component.literal(" (" + enchantmentName(enchantId.get()) + ")"));
        }
        return line;
    }

    private static Optional<String> enchantmentIdFrom(CompoundTag where) {
        if (where == null || where.isEmpty() || !where.contains("id")) return Optional.empty();
        String id = where.getString("id");
        return id.isEmpty() ? Optional.empty() : Optional.of(id);
    }

    // SNBT whitelist / blacklist ---------------------------------------------------------------

    private static void describeCompound(CompoundTag tag, List<Component> out) {
        for (String key : tag.getAllKeys()) {
            Tag value = tag.get(key);

            if (ENCHANTMENTS.equals(key) && value instanceof ListTag list) {
                describeEnchantmentList(list, out);
            } else if (DAMAGE.equals(key)) {
                out.add(Component.translatable(TranslationKeys.INTERACTION_NBT_DAMAGE)
                        .append(Component.literal(" " + valueOrRange(value))));
            } else if ("custom".equals(key) && value instanceof CompoundTag custom) {
                for (String customKey : custom.getAllKeys()) {
                    out.add(Component.literal(customKey + " " + valueOrRange(custom.get(customKey))));
                }
            } else {
                out.add(Component.literal(friendlyPath(key) + " " + valueOrRange(value)));
            }
        }
    }

    private static void describeEnchantmentList(ListTag list, List<Component> out) {
        for (Tag element : list) {
            if (!(element instanceof CompoundTag entry)) continue;

            String id = entry.getString("id");
            String name = id.isEmpty()
                    ? Component.translatable(TranslationKeys.INTERACTION_NBT_ANY_ENCHANT).getString()
                    : enchantmentName(id);

            Tag level = entry.get("lvl");
            if (level == null) {
                out.add(Component.literal(name).append(Component.translatable(TranslationKeys.INTERACTION_NBT_PRESENT)));
                continue;
            }

            readRange(level).ifPresentOrElse(
                    range -> out.add(Component.literal(name + " " + roman((int) range[0]) + " - " + roman((int) range[1]))),
                    () -> {
                        if (level instanceof NumericTag numeric) {
                            out.add(Component.literal(name + " " + roman(numeric.getAsInt())));
                        } else {
                            out.add(Component.literal(name));
                        }
                    }
            );
        }
    }

    // Formatting helpers -----------------------------------------------------------------------

    /** A {@code {RangeTag:[min,max]}} bound, whatever numeric width the file used. */
    private static Optional<long[]> readRange(Tag tag) {
        if (!(tag instanceof CompoundTag compound) || !(compound.get("RangeTag") instanceof ListTag range) || range.size() != 2) {
            return Optional.empty();
        }
        if (!(range.get(0) instanceof NumericTag min) || !(range.get(1) instanceof NumericTag max)) return Optional.empty();
        return Optional.of(new long[]{min.getAsLong(), max.getAsLong()});
    }

    private static String valueOrRange(Tag tag) {
        Optional<long[]> range = readRange(tag);
        if (range.isPresent()) {
            long[] bounds = range.get();
            return bounds[0] == bounds[1]
                    ? "= " + bounds[0]
                    : Component.translatable(TranslationKeys.INTERACTION_NBT_BETWEEN, bounds[0], bounds[1]).getString();
        }
        if (tag instanceof NumericTag numeric) return "= " + numeric.getAsLong();
        return tag == null ? "" : "= " + tag.getAsString();
    }

    private static String numericRange(Optional<Integer> min, Optional<Integer> max) {
        if (min.isEmpty() && max.isEmpty()) return "";
        if (min.isPresent() && max.isPresent()) {
            return min.get().equals(max.get())
                    ? "= " + min.get()
                    : Component.translatable(TranslationKeys.INTERACTION_NBT_BETWEEN, min.get(), max.get()).getString();
        }
        return min.map(value -> "≥ " + value).orElseGet(() -> "≤ " + max.get());
    }

    private static String romanRange(Optional<Integer> min, Optional<Integer> max) {
        if (min.isEmpty() && max.isEmpty()) return Component.translatable(TranslationKeys.INTERACTION_NBT_ANY_LEVEL).getString();
        if (min.isPresent() && max.isPresent()) {
            return min.get().equals(max.get()) ? roman(min.get()) : roman(min.get()) + " - " + roman(max.get());
        }
        return min.map(value -> "≥ " + roman(value)).orElseGet(() -> "≤ " + roman(max.get()));
    }

    /** {@code Enchantments[].lvl} → "Enchantments level"; {@code custom.foo} → "foo". */
    private static String friendlyPath(String path) {
        String cleaned = path.replace("[]", "");
        if (cleaned.startsWith("custom.")) cleaned = cleaned.substring("custom.".length());
        if (cleaned.equals(DAMAGE)) return Component.translatable(TranslationKeys.INTERACTION_NBT_DAMAGE).getString();

        cleaned = cleaned.replace('.', ' ');
        return cleaned.endsWith(" lvl") ? cleaned.substring(0, cleaned.length() - 4) + " level" : cleaned;
    }

    private static String enchantmentName(String enchantmentId) {
        ResourceLocation rl = ResourceLocation.tryParse(enchantmentId);
        if (rl == null) return enchantmentId;
        return Component.translatable("enchantment." + rl.getNamespace() + "." + rl.getPath()).getString();
    }

    private static String roman(int number) {
        if (number < 1 || number > 3999) return String.valueOf(number);

        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] numerals = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            while (number >= values[i]) {
                number -= values[i];
                result.append(numerals[i]);
            }
        }
        return result.toString();
    }
}
