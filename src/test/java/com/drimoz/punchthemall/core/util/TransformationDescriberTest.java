package com.drimoz.punchthemall.core.util;

import com.drimoz.punchthemall.McBootstrap;
import com.drimoz.punchthemall.core.model.classes.PtaBlock;
import com.drimoz.punchthemall.core.model.classes.PtaInteraction;
import com.drimoz.punchthemall.core.model.classes.PtaPool;
import com.drimoz.punchthemall.core.model.classes.PtaRewards;
import com.drimoz.punchthemall.core.model.classes.PtaTransformation;
import com.drimoz.punchthemall.core.model.enums.PtaDropMode;
import com.drimoz.punchthemall.core.model.enums.PtaTransformOp;
import com.drimoz.punchthemall.core.model.enums.PtaTypeEnum;
import com.drimoz.punchthemall.core.model.records.PtaOffset;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the tooltip actually says.
 *
 * <p>Worth pinning down because the first version of it was wrong in a way no test could have
 * caught and no compiler could complain about: it ended a tooltip with "+1 more transformation(s)"
 * and offered no way to see what that one was. The assertions here are about whether a player
 * learns anything, not about whether the code runs.</p>
 */
class TransformationDescriberTest {

    @BeforeAll
    static void bootstrap() {
        McBootstrap.ensure();
    }

    private static PtaInteraction interaction(double groupChance, PtaTransformation... transformations) {
        return new PtaInteraction(
                ResourceLocation.fromNamespaceAndPath("pta_test", "describe"),
                PtaTypeEnum.RIGHT_CLICK, null, null, null,
                PtaBlock.createBlock(Set.of(Blocks.STONE), null, null, null, null),
                List.of(transformations), groupChance,
                PtaRewards.of(PtaPool.create(null)), null, null, null, false, 0);
    }

    private static PtaTransformation place(net.minecraft.world.level.block.Block block, PtaOffset at) {
        return PtaTransformation.createBlock(1.0, block, null, null, null, null)
                .withPlacement(PtaTransformOp.PLACE, at, null, PtaDropMode.VANILLA);
    }

    private static PtaTransformation breaking(PtaOffset at, PtaDropMode drops) {
        return PtaTransformation.createAir(1.0, null, null)
                .withPlacement(PtaTransformOp.BREAK, at, null, drops);
    }

    private static PtaOffset at(int x, int y, int z) {
        return new PtaOffset(x, y, z, PtaOffset.Frame.WORLD);
    }

    /**
     * Key and arguments, not English.
     *
     * <p>There is no language file outside a client, so a translatable component renders as its
     * key. That is not a limitation worth fighting: the key and its arguments are what the mod
     * actually decides, and the English is a translator concern.</p>
     */
    private static String flatten(List<Component> lines) {
        StringBuilder text = new StringBuilder();
        for (Component line : lines) {
            render(line, text);
            text.append(" | ");
        }
        return text.toString();
    }

    private static void render(Component component, StringBuilder into) {
        if (component.getContents() instanceof TranslatableContents translatable) {
            into.append(translatable.getKey());
            for (Object arg : translatable.getArgs()) {
                into.append(" ");
                if (arg instanceof Component nested) render(nested, into);
                else into.append(arg);
            }
        } else {
            into.append(component.getString());
        }
        component.getSiblings().forEach(sibling -> render(sibling, into));
    }

    @Test
    @DisplayName("the summary names the block a transformation writes, not just that it writes one")
    void summaryNamesTheBlock() {
        String text = flatten(TransformationDescriber.describe(
                interaction(1.0, place(Blocks.TORCH, at(0, 1, 0))), false));

        assertTrue(text.contains("block.minecraft.torch"),
                "a player asking what it places deserves an answer, got: " + text);
        assertTrue(text.contains("+0 +1 +0"), "the offset should be there, got: " + text);
    }

    @Test
    @DisplayName("the detailed form names every transformation, in order")
    void detailNamesEveryTransformation() {
        PtaInteraction interaction = interaction(1.0,
                place(Blocks.TORCH, at(0, 1, 0)),
                place(Blocks.GLASS, at(0, 2, 0)));

        String summary = flatten(TransformationDescriber.describe(interaction, false));
        String detail = flatten(TransformationDescriber.describe(interaction, true));

        // The summary may mention only the first, but it must not be the only thing available.
        assertFalse(summary.contains("block.minecraft.glass"),
                "the summary stays short, got: " + summary);
        assertTrue(detail.contains("block.minecraft.torch"), "detail missing the first: " + detail);
        assertTrue(detail.contains("block.minecraft.glass"), "detail missing the second: " + detail);
        assertTrue(detail.contains("1."), "the detail should number them, got: " + detail);
        assertTrue(detail.contains("2."), "the detail should number them, got: " + detail);
    }

    @Test
    @DisplayName("anything the summary hides, the detail shows")
    void nothingIsHiddenWithoutARouteToIt() {
        PtaInteraction interaction = interaction(1.0,
                place(Blocks.TORCH, at(0, 1, 0)),
                breaking(at(0, -1, 0), PtaDropMode.TOOL));

        assertTrue(TransformationDescriber.hasMoreToShow(interaction),
                "with two transformations the viewer must offer the expanded form");

        String detail = flatten(TransformationDescriber.describe(interaction, true));
        assertTrue(detail.contains("+0 -1 +0"), "the second offset should be reachable, got: " + detail);
    }

    @Test
    @DisplayName("a single transformation has nothing more to show, so no hint is offered")
    void oneTransformationOffersNoExpansion() {
        assertFalse(TransformationDescriber.hasMoreToShow(interaction(1.0, place(Blocks.TORCH, at(0, 1, 0)))));
    }

    @Test
    @DisplayName("a group chance is stated, because it applies to everything below it")
    void groupChanceIsStated() {
        String text = flatten(TransformationDescriber.describe(
                interaction(0.7, place(Blocks.TORCH, at(0, 1, 0))), false));

        assertTrue(text.contains("70"), "the set-level chance should be visible, got: " + text);
    }

    @Test
    @DisplayName("a region says how many blocks it covers, since the offset alone does not")
    void regionSaysHowManyBlocks() {
        PtaOffset area = new PtaOffset(-1, 0, -1, PtaOffset.Frame.WORLD,
                new PtaOffset(1, 0, 1, PtaOffset.Frame.WORLD));

        String text = flatten(TransformationDescriber.describe(
                interaction(1.0, place(Blocks.GLASS, area)), false));

        assertTrue(text.contains("9"), "a 3x3 covers nine blocks and should say so, got: " + text);
    }

    @Test
    @DisplayName("tool drops read differently from plain drops")
    void toolDropsAreDistinguished() {
        String tool = flatten(TransformationDescriber.describe(
                interaction(1.0, breaking(at(0, 1, 0), PtaDropMode.TOOL)), false));
        String vanilla = flatten(TransformationDescriber.describe(
                interaction(1.0, breaking(at(0, 1, 0), PtaDropMode.VANILLA)), false));
        String none = flatten(TransformationDescriber.describe(
                interaction(1.0, breaking(at(0, 1, 0), PtaDropMode.NONE)), false));

        assertNotEqualsIgnoringNull(tool, vanilla);
        assertFalse(none.contains("transformation.drops"), "nothing is dropped, so say nothing: " + none);
    }

    private static void assertNotEqualsIgnoringNull(String a, String b) {
        assertFalse(a.equals(b), "a tool-aware break should not read the same as a plain one");
    }

    @Test
    @DisplayName("an interaction with no transformation says nothing at all")
    void nothingToSay() {
        assertEquals(List.of(), TransformationDescriber.describe(interaction(1.0), false));
        assertEquals(List.of(), TransformationDescriber.describe(interaction(1.0), true));
    }
}
