package com.drimoz.punchthemall.core.util;

import com.drimoz.punchthemall.core.model.classes.PtaBlock;
import com.drimoz.punchthemall.core.model.classes.PtaInteraction;
import com.drimoz.punchthemall.core.model.classes.PtaTransformation;
import com.drimoz.punchthemall.core.model.enums.PtaDropMode;
import com.drimoz.punchthemall.core.model.records.PtaOffset;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.List;

/**
 * Says, in the tooltip, what an interaction does to the world.
 *
 * <p>Both viewers draw a transformation as one output slot next to the target, which reads as "this
 * block becomes that". That is only true for a {@code replace} sitting on the clicked block. A
 * {@code break} destroys instead of writing, an offset lands somewhere else, a region covers nine
 * blocks, and a list does several things at once — none of which a lone slot can show.</p>
 *
 * <p>So the slot keeps its picture and the tooltip carries the rest, in two lengths. The
 * <b>summary</b> describes the first transformation and says how many others there are. The
 * <b>detail</b> numbers every one of them and gives each its own block of lines. Saying "and one
 * more" without ever offering the means to see it — which is what this did at first — tells the
 * player something is hidden and then hides it.</p>
 *
 * <p>Deliberately free of any client class so it can be unit tested; whether the player is asking
 * for the detail is decided by the caller.</p>
 */
public final class TransformationDescriber {

    private TransformationDescriber() {}

    /** Indent for the lines belonging to one numbered transformation. */
    private static final String INDENT = "  ";

    /**
     * @param detailed when false, describe the first transformation and count the rest; when true,
     *                 describe every one of them in order
     */
    public static List<Component> describe(PtaInteraction interaction, boolean detailed) {
        List<PtaTransformation> transformations = interaction.getTransformations();
        if (transformations.isEmpty()) return List.of();

        List<Component> lines = new ArrayList<>();
        addGroupChance(interaction, lines);

        if (!detailed) {
            describeOne(transformations.get(0), lines, "");
            if (transformations.size() > 1) {
                lines.add(Component.translatable(TranslationKeys.INTERACTION_TRANSFORMATION_MORE,
                                transformations.size() - 1)
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            }
            return lines;
        }

        if (transformations.size() == 1) {
            describeOne(transformations.get(0), lines, "");
            return lines;
        }

        lines.add(Component.translatable(TranslationKeys.INTERACTION_TRANSFORMATION_COUNT, transformations.size())
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        for (int index = 0; index < transformations.size(); index++) {
            PtaTransformation transformation = transformations.get(index);
            lines.add(Component.literal(INDENT + (index + 1) + ". ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(what(transformation).withStyle(ChatFormatting.LIGHT_PURPLE)));
            describeWhere(transformation, lines, INDENT + INDENT);
        }
        return lines;
    }

    /** Kept for callers that have not been given a mode; behaves as the summary. */
    public static List<Component> describe(PtaInteraction interaction) {
        return describe(interaction, false);
    }

    /** Whether the detailed form would actually say more than the summary. */
    public static boolean hasMoreToShow(PtaInteraction interaction) {
        return interaction.getTransformations().size() > 1;
    }

    // One transformation, unnumbered: what it does, then where and under what conditions.

    private static void describeOne(PtaTransformation transformation, List<Component> lines, String indent) {
        lines.add(Component.literal(indent).append(what(transformation).withStyle(ChatFormatting.LIGHT_PURPLE)));
        describeWhere(transformation, lines, indent);
    }

    private static void describeWhere(PtaTransformation transformation, List<Component> lines, String indent) {
        if (transformation.hasOffset()) {
            lines.add(Component.literal(indent)
                    .append(Component.translatable(TranslationKeys.INTERACTION_TRANSFORMATION_AT,
                                    describeOffset(transformation.getOffset()),
                                    Component.translatable(frameKey(transformation.getOffset())))
                            .withStyle(ChatFormatting.AQUA)));

            if (transformation.getOffset().isRegion()) {
                lines.add(Component.literal(indent)
                        .append(Component.translatable(TranslationKeys.INTERACTION_TRANSFORMATION_REGION,
                                        transformation.getOffset().size())
                                .withStyle(ChatFormatting.AQUA)));
            }
        }

        if (transformation.isBreak() && transformation.getDropMode() != PtaDropMode.NONE) {
            String key = transformation.getDropMode() == PtaDropMode.TOOL
                    ? TranslationKeys.INTERACTION_TRANSFORMATION_DROPS_TOOL
                    : TranslationKeys.INTERACTION_TRANSFORMATION_DROPS;
            lines.add(Component.literal(indent)
                    .append(Component.translatable(key).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)));
        }

        if (transformation.hasRequirement()) {
            String names = describeSelector(transformation.getRequire());
            if (!names.isEmpty()) {
                lines.add(Component.literal(indent)
                        .append(Component.translatable(TranslationKeys.INTERACTION_TRANSFORMATION_REQUIRE, names)
                                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)));
            }
        }
    }

    /**
     * What this transformation does, naming the block where there is one.
     *
     * <p>"Places a block" is what the first version said, and it is not an answer to "places what?".</p>
     */
    private static MutableComponent what(PtaTransformation transformation) {
        return switch (transformation.getOp()) {
            case BREAK -> Component.translatable(TranslationKeys.INTERACTION_TRANSFORMATION_OP_BREAK);
            case PLACE -> Component.translatable(TranslationKeys.INTERACTION_TRANSFORMATION_OP_PLACE_NAMED, written(transformation));
            case REPLACE -> Component.translatable(TranslationKeys.INTERACTION_TRANSFORMATION_OP_REPLACE_NAMED, written(transformation));
        };
    }

    private static Component written(PtaTransformation transformation) {
        if (transformation.isCopy()) {
            return Component.translatable(TranslationKeys.INTERACTION_TRANSFORMATION_COPIED);
        }
        if (transformation.isBlock()) {
            return transformation.getBlock().getName();
        }
        if (transformation.isFluid()) {
            return bucketName(transformation.getFluid());
        }
        return Component.translatable(TranslationKeys.INTERACTION_TRANSFORMATION_BREAK);
    }

    private static void addGroupChance(PtaInteraction interaction, List<Component> lines) {
        double chance = interaction.getTransformationChance();
        if (chance >= 1.0D || interaction.getTransformations().isEmpty()) return;

        lines.add(Component.translatable(TranslationKeys.INTERACTION_TRANSFORMATION_GROUP,
                        String.format("%.1f", chance * 100))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private static String frameKey(PtaOffset offset) {
        return switch (offset.frame()) {
            case WORLD -> TranslationKeys.INTERACTION_TRANSFORMATION_FRAME_WORLD;
            case PLAYER -> TranslationKeys.INTERACTION_TRANSFORMATION_FRAME_PLAYER;
            case FACE -> TranslationKeys.INTERACTION_TRANSFORMATION_FRAME_FACE;
        };
    }

    /** Signed on every axis, so the direction is readable without counting minus signs. */
    private static String describeOffset(PtaOffset offset) {
        String near = signed(offset.x()) + " " + signed(offset.y()) + " " + signed(offset.z());
        if (!offset.isRegion()) return near;
        return near + " → " + signed(offset.to().x()) + " " + signed(offset.to().y()) + " " + signed(offset.to().z());
    }

    private static String signed(int value) {
        return value >= 0 ? "+" + value : String.valueOf(value);
    }

    /** A short, readable list of what the destination must be. Long sets are truncated. */
    private static String describeSelector(PtaBlock selector) {
        List<String> names = new ArrayList<>();
        for (Block block : selector.getBlockSet()) {
            names.add(block.getName().getString());
        }
        for (Fluid fluid : selector.getFluidSet()) {
            names.add(bucketName(fluid).getString());
        }
        if (names.isEmpty()) return "";

        names.sort(String::compareTo);
        if (names.size() <= 3) return String.join(", ", names);
        return String.join(", ", names.subList(0, 3)) + ", +" + (names.size() - 3);
    }

    /**
     * A fluid named by its bucket.
     *
     * <p>26.1 removed Item.getDescription; the item names itself from a stack now. A default stack is
     * enough here, since nothing has renamed it.</p>
     */
    private static Component bucketName(Fluid fluid) {
        Item bucket = fluid.getBucket();
        return bucket.getName(new ItemStack(bucket));
    }
}
