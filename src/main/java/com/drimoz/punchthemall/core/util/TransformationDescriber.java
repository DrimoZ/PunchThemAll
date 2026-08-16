package com.drimoz.punchthemall.core.util;

import com.drimoz.punchthemall.core.model.classes.PtaBlock;
import com.drimoz.punchthemall.core.model.classes.PtaInteraction;
import com.drimoz.punchthemall.core.model.classes.PtaTransformation;
import com.drimoz.punchthemall.core.model.records.PtaOffset;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.List;

/**
 * Says, in the tooltip, what a transformation actually does.
 *
 * <p>Both viewers draw a transformation as one output slot next to the target, which reads as
 * "this block becomes that". That is only true for a {@code replace} sitting on the clicked block.
 * A {@code break} destroys instead of writing, an offset lands somewhere else entirely, and a list
 * does several things at once — none of which a lone slot can show. So the slot keeps its picture
 * and the tooltip carries the rest, worded the same way in JEI and EMI.</p>
 */
public final class TransformationDescriber {

    private TransformationDescriber() {}

    /** The lines describing an interaction's transformations, beyond the chance the slot already shows. */
    public static List<Component> describe(PtaInteraction interaction) {
        List<PtaTransformation> transformations = interaction.getTransformations();
        if (transformations.isEmpty()) return List.of();

        List<Component> lines = new ArrayList<>();
        describeOne(transformations.get(0), lines);

        // Only the first has a slot of its own; the others would otherwise be invisible.
        if (transformations.size() > 1) {
            lines.add(Component.translatable(TranslationKeys.INTERACTION_TRANSFORMATION_MORE, transformations.size() - 1)
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
        return lines;
    }

    private static void describeOne(PtaTransformation transformation, List<Component> lines) {
        lines.add(Component.translatable(opKey(transformation)).withStyle(ChatFormatting.LIGHT_PURPLE));

        if (transformation.isBreak() && transformation.shouldDropItems()) {
            lines.add(Component.translatable(TranslationKeys.INTERACTION_TRANSFORMATION_DROPS)
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }

        if (transformation.hasOffset()) {
            lines.add(Component.translatable(TranslationKeys.INTERACTION_TRANSFORMATION_AT,
                            describeOffset(transformation.getOffset()),
                            Component.translatable(frameKey(transformation.getOffset())))
                    .withStyle(ChatFormatting.AQUA));
        }

        if (transformation.hasRequirement()) {
            String names = describeRequirement(transformation.getRequire());
            if (!names.isEmpty()) {
                lines.add(Component.translatable(TranslationKeys.INTERACTION_TRANSFORMATION_REQUIRE, names)
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            }
        }
    }

    private static String opKey(PtaTransformation transformation) {
        return switch (transformation.getOp()) {
            case BREAK -> TranslationKeys.INTERACTION_TRANSFORMATION_OP_BREAK;
            case PLACE -> TranslationKeys.INTERACTION_TRANSFORMATION_OP_PLACE;
            case REPLACE -> TranslationKeys.INTERACTION_TRANSFORMATION_OP_REPLACE;
        };
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
        return signed(offset.x()) + " " + signed(offset.y()) + " " + signed(offset.z());
    }

    private static String signed(int value) {
        return value >= 0 ? "+" + value : String.valueOf(value);
    }

    /** A short, readable list of what the destination must be. Long sets are truncated. */
    private static String describeRequirement(PtaBlock require) {
        List<String> names = new ArrayList<>();
        for (Block block : require.getBlockSet()) {
            names.add(block.getName().getString());
        }
        for (Fluid fluid : require.getFluidSet()) {
            names.add(fluid.getBucket().getDescription().getString());
        }
        if (names.isEmpty()) return "";

        names.sort(String::compareTo);
        if (names.size() <= 3) return String.join(", ", names);
        return String.join(", ", names.subList(0, 3)) + ", +" + (names.size() - 3);
    }
}
