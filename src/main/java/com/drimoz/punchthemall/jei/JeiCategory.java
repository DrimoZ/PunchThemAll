package com.drimoz.punchthemall.jei;

import com.drimoz.punchthemall.PunchThemAll;
import com.drimoz.punchthemall.core.model.DropEntry;
import com.drimoz.punchthemall.core.model.EInteractionHand;
import com.drimoz.punchthemall.core.model.EInteractionType;
import com.drimoz.punchthemall.core.model.Interaction;
import com.drimoz.punchthemall.core.registry.InteractionRegistry;
import com.drimoz.punchthemall.core.util.PTALoggers;
import com.drimoz.punchthemall.core.util.TranslationKeys;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class JeiCategory implements IRecipeCategory<Interaction> {
    public static final ResourceLocation JEI_TEXTURE = new ResourceLocation(PunchThemAll.MOD_ID, "textures/gui/jei.png");
    public static final ResourceLocation UID = new ResourceLocation(PunchThemAll.MOD_ID, "interactions");

    public static final int WIDTH = 162;
    public static final int HEIGHT_START = 44;

    public static final int X_MOUSE_ICON = 0;
    public static final int Y_MOUSE_ICON = HEIGHT_START - 18 * 2 - 3 - 5;

    public static final int X_SNEAK_ICON = 0;
    public static final int Y_SNEAK_ICON = HEIGHT_START - 18 - 5;

    public static final int X_HAND_ICON = WIDTH - 18 * 6 - 5;
    public static final int Y_HAND_ICON = Y_MOUSE_ICON;

    public static final int X_ARROW = WIDTH - 18 * 4 - 4;
    public static final int Y_ARROW = Y_MOUSE_ICON + 4;

    public static final int X_HAND_ITEM = WIDTH - 18 * 5 - 5;
    public static final int Y_HAND_ITEM = Y_MOUSE_ICON;


    public static final int X_BLOCK = WIDTH - 18 * 4;
    public static final int Y_BLOCK = Y_SNEAK_ICON;

    public static final int X_TRANSFORMATION = WIDTH - 18;
    public static final int Y_TRANSFORMATION = Y_SNEAK_ICON;

    private final IDrawable ICON;
    private final IDrawable BACKGROUND;

    private final IDrawable SLOT;
    private final IDrawable SLOT_ROW;

    private final IDrawable ARROW;

    private final IDrawable MOUSE_RIGHT_CLICK;
    private final IDrawable MOUSE_LEFT_CLICK;

    private final IDrawable SNEAK_CLICK;
    private final IDrawable REGULAR_CLICK;

    private final IDrawable ANY_HAND;
    private final IDrawable MAIN_HAND;
    private final IDrawable OFF_HAND;

    // Life Cycle

    public JeiCategory(IGuiHelper guiHelper) {
        this.ICON = guiHelper.createDrawableItemStack(new ItemStack(Items.DANDELION));
        this.BACKGROUND = guiHelper.createBlankDrawable(WIDTH, HEIGHT_START + 18 * InteractionRegistry.getInstance().getJEIRowCount());

        this.SLOT = guiHelper.getSlotDrawable();
        this.SLOT_ROW = guiHelper.createDrawable(JEI_TEXTURE, 0, 0, 162, 18);

        this.ARROW = guiHelper.createDrawable(JEI_TEXTURE, 36, 18, 18, 18);

        this.MOUSE_RIGHT_CLICK = guiHelper.createDrawable(JEI_TEXTURE, 0, 18, 18, 18);
        this.MOUSE_LEFT_CLICK = guiHelper.createDrawable(JEI_TEXTURE, 18, 18, 18, 18);

        this.SNEAK_CLICK = guiHelper.createDrawable(JEI_TEXTURE, 126, 18, 18, 18);
        this.REGULAR_CLICK = guiHelper.createDrawable(JEI_TEXTURE, 108, 18, 18, 18);

        this.ANY_HAND = guiHelper.createDrawable(JEI_TEXTURE, 54, 18, 18, 18);
        this.MAIN_HAND = guiHelper.createDrawable(JEI_TEXTURE, 90, 18, 18, 18);
        this.OFF_HAND = guiHelper.createDrawable(JEI_TEXTURE, 72, 18, 18, 18);
    }

    // Interface

    @Override
    public RecipeType<Interaction> getRecipeType() {
        return JEIPlugin.INTERACTION_RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable(TranslationKeys.CATEGORY_TITLE);
    }

    @Override
    public IDrawable getBackground() {
        return BACKGROUND;
    }

    @Override
    public IDrawable getIcon() {
        return ICON;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Interaction interaction, IFocusGroup focuses) {
        // Set up hand item slot
        builder.addSlot(RecipeIngredientRole.INPUT, 1 + X_HAND_ITEM, 1 + Y_HAND_ITEM)
                .addItemStack(getHandItemStack(interaction));

        // Set up block item slot
        builder.addSlot(RecipeIngredientRole.INPUT, 1 + X_BLOCK, 1 + Y_BLOCK)
                .addItemStack(getBlockItemStack(interaction));

        // Set up transformation slot if applicable
        if (interaction.getInteractedBlock().getTransformationChance() > 0) {
            var transformationSlot = builder.addSlot(RecipeIngredientRole.OUTPUT, 1 + X_TRANSFORMATION, 1 + Y_TRANSFORMATION)
                    .addItemStack(getTransformationItemStack(interaction));

            transformationSlot.addTooltipCallback((slotView, tooltip) -> tooltip.add(Component.literal("§o§8" + Component.translatable(TranslationKeys.INTERACTION_OUTPUT_CHANCE).getString() +
                    " : §l§5" + getTruncatedChance(interaction.getInteractedBlock().getTransformationChance(), 0, 1) + "%")));
        }

        // Set up drop slots
        int slotNumber = 0;
        for (DropEntry result : interaction.getDropPool()) {
            if (!result.getItemStack().is(Items.AIR)) {
                var slot = builder.addSlot(RecipeIngredientRole.OUTPUT, 1 + (slotNumber % 9) * 18, 1 + HEIGHT_START + 18 * (slotNumber / 9))
                        .addItemStack(result.getItemStack());

                int totalWeight = interaction.getDropPool().stream().mapToInt(DropEntry::getChance).sum();
                slot.addTooltipCallback((slotView, tooltip) -> tooltip.add(Component.literal("§o§8" + Component.translatable(TranslationKeys.INTERACTION_OUTPUT_CHANCE).getString() +
                        " : §l§5" + getTruncatedChance(result.getChance(), 0, totalWeight) + "%")));

                slotNumber++;
            }
        }
    }

    @Override
    public void draw(Interaction interaction, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        boolean isLeftClick = interaction.getInteractionType() == EInteractionType.LEFT_CLICK ||
                interaction.getInteractionType() == EInteractionType.SHIFT_LEFT_CLICK;

        boolean isShiftClick = interaction.getInteractionType() == EInteractionType.SHIFT_RIGHT_CLICK ||
                interaction.getInteractionType() == EInteractionType.SHIFT_LEFT_CLICK;

        // Draw mouse icon
        IDrawable mouseIcon = isLeftClick ? MOUSE_LEFT_CLICK : MOUSE_RIGHT_CLICK;
        mouseIcon.draw(graphics, X_MOUSE_ICON, Y_MOUSE_ICON);

        // Draw sneak icon
        IDrawable sneakIcon = isShiftClick ? SNEAK_CLICK : REGULAR_CLICK;
        sneakIcon.draw(graphics, X_SNEAK_ICON, Y_SNEAK_ICON);

        if (interaction.getInteractionHand() != null) {
            // Draw sneak icon
            IDrawable handIcon = interaction.getInteractionHand().getInteractionHandType().equals(EInteractionHand.ANY_HAND) ? ANY_HAND : interaction.getInteractionHand().getInteractionHandType().equals(EInteractionHand.MAIN_HAND) ? MAIN_HAND : OFF_HAND;
            handIcon.draw(graphics, X_HAND_ICON, Y_HAND_ICON);
        }

        // Draw icons and slots
        SLOT.draw(graphics, X_HAND_ITEM, Y_HAND_ITEM);
        ARROW.draw(graphics, X_ARROW, Y_ARROW);
        SLOT.draw(graphics, X_BLOCK, Y_BLOCK);

        if (interaction.getInteractedBlock().getTransformationChance() > 0) {
            SLOT.draw(graphics, X_TRANSFORMATION, Y_TRANSFORMATION);
        }

        // Draw rows
        for (int i = 0; i < interaction.getJeiRowCount(); i++) {
            SLOT_ROW.draw(graphics, 0, HEIGHT_START + i * 18);
        }

        // Tooltip for the mouse icon
        if (isMouseOver(mouseX, mouseY, X_MOUSE_ICON + 1, Y_MOUSE_ICON + 1, 16, 16)) {
            graphics.renderTooltip(Minecraft.getInstance().font, Component.translatable(isLeftClick ? TranslationKeys.INTERACTION_CLICK_LEFT : TranslationKeys.INTERACTION_CLICK_RIGHT), (int) mouseX, (int) mouseY);
        }

        // Tooltip for the sneak icon
        if (isMouseOver(mouseX, mouseY, X_SNEAK_ICON + 1, Y_SNEAK_ICON + 1, 16, 16)) {
            graphics.renderTooltip(Minecraft.getInstance().font, Component.translatable(isShiftClick ? TranslationKeys.INTERACTION_POSITION_SNEAK : TranslationKeys.INTERACTION_POSITION_UP), (int) mouseX, (int) mouseY);
        }

        // Tooltip for the sneak icon
        if (interaction.getInteractionHand() != null && isMouseOver(mouseX, mouseY, X_HAND_ICON + 1, Y_HAND_ICON + 1, 16, 16)) {
            graphics.renderTooltip(Minecraft.getInstance().font, Component.translatable(interaction.getInteractionHand().getInteractionHandType().equals(EInteractionHand.ANY_HAND) ? TranslationKeys.INTERACTION_HAND_ANY : interaction.getInteractionHand().getInteractionHandType().equals(EInteractionHand.MAIN_HAND) ? TranslationKeys.INTERACTION_HAND_MAIN : TranslationKeys.INTERACTION_HAND_OFF), (int) mouseX, (int) mouseY);
        }
    }

    // Inner Work

    private boolean isMouseOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private ItemStack getHandItemStack(Interaction interaction) {
        if (interaction.getInteractionHand() != null && interaction.getInteractionHand().getItemStack() != null) {
            return interaction.getInteractionHand().getItemStack();
        } else {
            return new ItemStack(Items.BARRIER).setHoverName(Component.literal("§d " + Component.translatable(TranslationKeys.INTERACTION_HAND_NO_ITEM).getString()));
        }
    }

    private ItemStack getBlockItemStack(Interaction interaction) {
        if (interaction.getInteractedBlock().isAir()) {
            return new ItemStack(Items.BARRIER).setHoverName(Component.literal("§d " + Component.translatable(TranslationKeys.INTERACTION_BLOCK_WITH_AIR).getString()));
        } else if (interaction.getInteractedBlock().getBlockBase().isBlock()) {
            return new ItemStack(interaction.getInteractedBlock().getBlockBase().getBlock().asItem());
        } else {
            return new ItemStack(interaction.getInteractedBlock().getBlockBase().getFluid().getBucket());
        }
    }

    private ItemStack getTransformationItemStack(Interaction interaction) {
        if (interaction.getInteractedBlock().isTransformedAir()) {
            return new ItemStack(Items.BARRIER).setHoverName(Component.literal("§d " + Component.translatable(TranslationKeys.INTERACTION_TRANSFORMATION_BREAK).getString()));
        } else if (interaction.getInteractedBlock().getTransformedBase().isBlock()) {
            return new ItemStack(interaction.getInteractedBlock().getTransformedBase().getBlock().asItem());
        } else {
            return new ItemStack(interaction.getInteractedBlock().getTransformedBase().getFluid().getBucket());
        }
    }

    private static double getTruncatedChance(double num, double min, double max) {
        double chance = (num - min) / (max - min) * 100;

        double truncatedChance = Math.floor(chance * 1_000_000) / 1_000_000; // Truncate to 6 decimal places for safety
        truncatedChance = Math.floor(truncatedChance * 100_000) / 100_000;

        if (truncatedChance == 0.0) {
            truncatedChance = 0.00001;
        }

        return truncatedChance;
    }
}
