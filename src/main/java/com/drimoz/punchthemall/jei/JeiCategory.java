package com.drimoz.punchthemall.jei;

import com.drimoz.punchthemall.core.model.classes.PtaBlock;
import com.drimoz.punchthemall.core.model.classes.PtaHand;
import com.drimoz.punchthemall.core.model.classes.PtaInteraction;
import com.drimoz.punchthemall.core.model.classes.PtaTransformation;
import com.drimoz.punchthemall.core.model.records.PtaDropRecord;
import com.drimoz.punchthemall.core.model.records.PtaStateRecord;
import com.drimoz.punchthemall.core.model.enums.PtaHandEnum;
import com.drimoz.punchthemall.core.registry.InteractionRegistry;
import com.drimoz.punchthemall.core.util.TranslationKeys;
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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.material.Fluid;

import java.util.*;

import static com.drimoz.punchthemall.jei.JeiConstants.*;

public class JeiCategory implements IRecipeCategory<PtaInteraction> {

    // Private Properties

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

    private final IDrawable BIOME_WHITELIST;
    private final IDrawable BIOME_BLACKLIST;

    // Life Cycle

    public JeiCategory(IGuiHelper guiHelper) {
        this.ICON = guiHelper.drawableBuilder(JEI_ICON_TEXTURE, 0, 0, 17, 17)
                .setTextureSize(17,17)
                .build();
        this.BACKGROUND = guiHelper.createBlankDrawable(JeiConstants.WIDTH, JeiConstants.HEIGHT_START + 18 * InteractionRegistry.getInstance().getJEIRowCount());

        this.SLOT = guiHelper.getSlotDrawable();
        this.SLOT_ROW = guiHelper.createDrawable(JeiConstants.JEI_TEXTURE, 0, 0, 162, 18);

        this.ARROW = guiHelper.createDrawable(JeiConstants.JEI_TEXTURE, 36, 18, 18, 18);

        this.MOUSE_RIGHT_CLICK = guiHelper.createDrawable(JeiConstants.JEI_TEXTURE, 18, 18, 18, 18);
        this.MOUSE_LEFT_CLICK = guiHelper.createDrawable(JeiConstants.JEI_TEXTURE, 0, 18, 18, 18);

        this.SNEAK_CLICK = guiHelper.createDrawable(JeiConstants.JEI_TEXTURE, 126, 18, 18, 18);
        this.REGULAR_CLICK = guiHelper.createDrawable(JeiConstants.JEI_TEXTURE, 108, 18, 18, 18);

        this.ANY_HAND = guiHelper.createDrawable(JeiConstants.JEI_TEXTURE, 54, 18, 18, 18);
        this.MAIN_HAND = guiHelper.createDrawable(JeiConstants.JEI_TEXTURE, 90, 18, 18, 18);
        this.OFF_HAND = guiHelper.createDrawable(JeiConstants.JEI_TEXTURE, 72, 18, 18, 18);

        this.BIOME_WHITELIST = guiHelper.createDrawable(JeiConstants.JEI_TEXTURE, 0, 36, 18, 18);
        this.BIOME_BLACKLIST = guiHelper.createDrawable(JeiConstants.JEI_TEXTURE, 18, 36, 18, 18);
    }

    // Interface

    @Override
    public RecipeType<PtaInteraction> getRecipeType() {
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
    public void setRecipe(IRecipeLayoutBuilder builder, PtaInteraction interaction, IFocusGroup focuses) {
        setupInteractionSlots(builder, interaction);
        setupDropSlots(builder, interaction);
    }

    @Override
    public void draw(PtaInteraction interaction, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        drawIcons(interaction, graphics);
        drawSlots(interaction, graphics);
        drawTooltips(interaction, mouseX, mouseY, graphics);
    }

    // Inner Work ( Slot )

    private void setupInteractionSlots(IRecipeLayoutBuilder builder, PtaInteraction interaction) {
        var handSlot = setupInputSlot(builder, getHandItemStack(interaction.getHand()), 1 + X_HAND_ITEM, 1 + Y_HAND_ITEM);
        if (!interaction.getHand().isEmpty()) {
            handSlot.addTooltipCallback(
                    (recipeSlotView, tooltip) ->
                            addStateAndNbtTooltip(tooltip,
                                    new HashSet<>(0), new HashSet<>(0),
                                    interaction.getHand().getNbtWhiteList(), interaction.getHand().getNbtBlackList()));
        }
        var blockSlot = setupInputSlot(builder, getBlockItemStack(interaction.getBlock()), 1 + X_BLOCK, 1 + Y_BLOCK);
        if (!interaction.getBlock().isAir()) {
            blockSlot.addTooltipCallback(
                    (recipeSlotView, tooltip) ->
                            addStateAndNbtTooltip(tooltip,
                                    interaction.getBlock().getStateWhiteList(), interaction.getBlock().getStateBlackList(),
                                    interaction.getBlock().getNbtWhiteList(), interaction.getBlock().getNbtBlackList()));
        }

        if (!interaction.getBlock().isAir() && interaction.getTransformation().hasTransformation()) {
            setupTransformationSlot(builder, interaction);
        }
    }

    private void setupDropSlots(IRecipeLayoutBuilder builder, PtaInteraction interaction) {
        int slotNumber = 0;
        int totalWeight = interaction.getPool().getTotalPoolWeight();

        for (Map.Entry<PtaDropRecord, Integer> result : interaction.getPool().getDropPool().entrySet()) {
            if (!result.getKey().isEmpty()) {
                setupDropSlot(builder, result, slotNumber, totalWeight);
                slotNumber++;
            }
        }
    }

    private void setupTransformationSlot(IRecipeLayoutBuilder builder, PtaInteraction interaction) {
        var transformationSlot = setupOutputSlot(builder, getTransformationItemStack(interaction.getTransformation()), 1 + X_TRANSFORMATION, 1 + Y_TRANSFORMATION);

        transformationSlot.addTooltipCallback((slotView, tooltip) -> {
            double chance = interaction.getTransformation().getChance();
            tooltip.add(Component.literal("§o§8" + Component.translatable(TranslationKeys.INTERACTION_TRANSFORMATION_CHANCE).getString() +
                    " : §l§5" + getTruncatedChance(chance, 0, 1) + "%"));
        });
    }

    private void setupDropSlot(IRecipeLayoutBuilder builder, Map.Entry<PtaDropRecord, Integer> result, int slotNumber, int totalPoolWeight) {
        var slot = setupOutputSlot(builder, result.getKey().getItemStack(), 1 + (slotNumber % 9) * 18, 1 + HEIGHT_START + 18 * (slotNumber / 9));

        slot.addTooltipCallback((slotView, tooltip) -> {
            tooltip.add(Component.literal("§o§8" + Component.translatable(TranslationKeys.INTERACTION_OUTPUT_CHANCE).getString() +
                    " : §l§5" + getTruncatedChance(result.getValue(), 0, totalPoolWeight) + "%"));
        });
    }

    private IRecipeSlotBuilder setupInputSlot(IRecipeLayoutBuilder builder, List<ItemStack> itemStacks, int x, int y) {
        return builder.addSlot(RecipeIngredientRole.INPUT, x, y).addItemStacks(itemStacks);
    }

    private IRecipeSlotBuilder setupOutputSlot(IRecipeLayoutBuilder builder, ItemStack itemStack, int x, int y) {
        return builder.addSlot(RecipeIngredientRole.OUTPUT, x, y)
                .addItemStack(itemStack);
    }

    // Inner Work ( ItemStack )

    private List<ItemStack> getHandItemStack(PtaHand hand) {
        if (!hand.isEmpty()) {
            return hand.getStacks();
        } else {
            return List.of(new ItemStack(Items.BARRIER).setHoverName(Component.literal("§d" + Component.translatable(TranslationKeys.INTERACTION_HAND_NO_ITEM).getString())));
        }
    }

    private List<ItemStack> getBlockItemStack(PtaBlock block) {
        if (block.isAir()) {
            return List.of(new ItemStack(Items.BARRIER).setHoverName(Component.literal("§d" + Component.translatable(TranslationKeys.INTERACTION_BLOCK_WITH_AIR).getString())));
        } else if (block.isBlock()) {
            return block.getBlockStacks();
        } else {
            return block.getFluidStacks();
        }
    }

    private ItemStack getTransformationItemStack(PtaTransformation transformation) {
        if (transformation.isAir()) {
            return new ItemStack(Items.BARRIER).setHoverName(Component.literal("§d" + Component.translatable(TranslationKeys.INTERACTION_TRANSFORMATION_BREAK).getString()));
        } else if (transformation.isBlock()) {
            return new ItemStack(transformation.getBlock());
        } else {
            return new ItemStack(transformation.getFluid().getBucket());
        }
    }

    // Inner Work ( Draw )

    private void drawIcons(PtaInteraction interaction, GuiGraphics graphics) {
        IDrawable mouseIcon = interaction.getType().isLeftClick() ? MOUSE_LEFT_CLICK : MOUSE_RIGHT_CLICK;
        mouseIcon.draw(graphics, X_MOUSE_ICON, Y_MOUSE_ICON);

        IDrawable sneakIcon = interaction.getType().isShiftClick() ? SNEAK_CLICK : REGULAR_CLICK;
        sneakIcon.draw(graphics, X_SNEAK_ICON, Y_SNEAK_ICON);

        if (!interaction.getHand().isEmpty()) {
            IDrawable handIcon = switch (interaction.getHand().getHand()) {
                case ANY_HAND -> ANY_HAND;
                case MAIN_HAND -> MAIN_HAND;
                case OFF_HAND -> OFF_HAND;
            };
            handIcon.draw(graphics, X_HAND_ICON, Y_HAND_ICON);
        }

        if (interaction.hasBiomeBlackList() || interaction.hasBiomeWhiteList()) {
            IDrawable biomeIcon = interaction.hasBiomeWhiteList() ? BIOME_WHITELIST : BIOME_BLACKLIST;
            biomeIcon.draw(graphics, X_BIOMES, Y_BIOMES);
        }

        ARROW.draw(graphics, X_ARROW, Y_ARROW);
    }

    private void drawSlots(PtaInteraction interaction, GuiGraphics graphics) {
        SLOT.draw(graphics, X_HAND_ITEM, Y_HAND_ITEM);
        SLOT.draw(graphics, X_BLOCK, Y_BLOCK);

        if (interaction.getTransformation().hasTransformation()) {
            SLOT.draw(graphics, X_TRANSFORMATION, Y_TRANSFORMATION);
        }

        for (int i = 0; i < interaction.getPool().getJEIRowCount(); i++) {
            SLOT_ROW.draw(graphics, 0, HEIGHT_START + i * 18);
        }
    }

    private void drawTooltips(PtaInteraction interaction, double mouseX, double mouseY, GuiGraphics graphics) {
        if (isMouseOver(mouseX, mouseY, X_MOUSE_ICON + 1, Y_MOUSE_ICON + 1, 16, 16)) {
            graphics.renderTooltip(
                    Minecraft.getInstance().font,
                    Component.translatable(
                            interaction.getType().isLeftClick() ?
                                    TranslationKeys.INTERACTION_CLICK_LEFT :
                                    TranslationKeys.INTERACTION_CLICK_RIGHT),
                    (int) mouseX,
                    (int) mouseY);
        }

        if (isMouseOver(mouseX, mouseY, X_SNEAK_ICON + 1, Y_SNEAK_ICON + 1, 16, 16)) {
            graphics.renderTooltip(
                    Minecraft.getInstance().font,
                    Component.translatable(
                            interaction.getType().isShiftClick() ?
                                    TranslationKeys.INTERACTION_POSITION_SNEAK :
                                    TranslationKeys.INTERACTION_POSITION_UP),
                    (int) mouseX,
                    (int) mouseY);
        }

        if (!interaction.getHand().isEmpty() && isMouseOver(mouseX, mouseY, X_HAND_ICON + 1, Y_HAND_ICON + 1, 16, 16)) {
            graphics.renderTooltip(
                    Minecraft.getInstance().font,
                    Component.translatable(getHandTranslationKey(interaction.getHand().getHand())),
                    (int) mouseX,
                    (int) mouseY);
        }

        if ((interaction.hasBiomeWhiteList() || interaction.hasBiomeBlackList()) && isMouseOver(mouseX, mouseY, X_BIOMES + 1, Y_BIOMES + 1, 16, 16)) {
            List<Component> tooltipComponents = new ArrayList<>();
            tooltipComponents.add(Component.literal("§7" +
                    Component.translatable(interaction.hasBiomeWhiteList() ?
                            TranslationKeys.INTERACTION_BIOME_WHITELIST :
                            TranslationKeys.INTERACTION_BIOME_BLACKLIST).getString() +
                    " : "));

            if (interaction.hasBiomeWhiteList()) {
                for (String biome : interaction.getBiomeWhitelist()) {
                    tooltipComponents.add(Component.literal("§6- " + biome.toLowerCase()));
                }
            } else {
                for (String biome : interaction.getBiomeBlackList()) {
                    tooltipComponents.add(Component.literal("§6- " + biome.toLowerCase()));
                }
            }

            graphics.renderTooltip(Minecraft.getInstance().font, tooltipComponents, Optional.empty(), (int) mouseX, (int) mouseY);
        }
    }

    // Inner Work ( Util )

    private boolean isMouseOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private double getTruncatedChance(double num, double min, double max) {
        double chance = (num - min) / (max - min) * 100;
        return Math.floor(chance * 1_000_000) / 1_000_000;
    }

    private String getHandTranslationKey(PtaHandEnum hand) {
        switch (hand) {
            case ANY_HAND:
                return TranslationKeys.INTERACTION_HAND_ANY;
            case MAIN_HAND:
                return TranslationKeys.INTERACTION_HAND_MAIN;
            case OFF_HAND:
                return TranslationKeys.INTERACTION_HAND_OFF;
            default:
                throw new IllegalArgumentException("Unknown hand type: " + hand);
        }
    }

    private void addStateAndNbtTooltip(List<Component> tooltip, Set<PtaStateRecord<?>> whitelistStates, Set<PtaStateRecord<?>> blacklistStates, CompoundTag whitelistNbt, CompoundTag blacklistNbt) {
        boolean isShiftKeyDown = Minecraft.getInstance().options.keyShift.isDown();
        boolean isSprintKeyDown = Minecraft.getInstance().options.keySprint.isDown();

        //if (!isShiftKeyDown) {
        //    tooltip.add(
        //            Component.literal(
        //                    "§b" + Component.translatable(TranslationKeys.TEXT_HOLD).getString() + " [ §6" +
        //                            Minecraft.getInstance().options.keyShift.getTranslatedKeyMessage().getString() + " §b] " +
        //                            Component.translatable(TranslationKeys.TEXT_DISPLAY).getString() + " " +
        //                            Component.translatable(TranslationKeys.TEXT_STATE).getString()
        //            )
        //    );
        //}
        //if (!isSprintKeyDown) {
        //    tooltip.add(
        //            Component.literal(
        //                    "§b" + Component.translatable(TranslationKeys.TEXT_HOLD).getString() + " [ §6" +
        //                            Minecraft.getInstance().options.keySprint.getTranslatedKeyMessage().getString() + " §b] " +
        //                            Component.translatable(TranslationKeys.TEXT_DISPLAY).getString() + " "  +
        //                            Component.translatable(TranslationKeys.TEXT_NBT).getString()
        //            )
        //    );
        //}

        // Add state tooltip if entries are present
        if (!whitelistStates.isEmpty() || !blacklistStates.isEmpty()) {
            tooltip.add(Component.literal(""));
            tooltip.add(Component.literal("§6State :"));

            if (!whitelistStates.isEmpty()) {
                tooltip.add(Component.literal(" Whitelist :"));
                for (PtaStateRecord<?> state : whitelistStates) {
                    tooltip.add(Component.literal("§8  - " + state.property().getName() + " : §5" + state.value().toString()));
                }
            }

            if (!blacklistStates.isEmpty()) {
                tooltip.add(Component.literal(" Blacklist :"));
                for (PtaStateRecord<?> state : whitelistStates) {
                    tooltip.add(Component.literal("§8  - " + state.property().getName() + " : §5" + state.value().toString()));
                }
            }
        }

        // Add NBT tooltip if NBT tag is present
        if (!whitelistStates.isEmpty() || !blacklistStates.isEmpty()) {
            tooltip.add(Component.literal(""));
            tooltip.add(Component.literal("§6NBT :"));

            if (!whitelistNbt.isEmpty()) {
                tooltip.add(Component.literal(" Whitelist :"));
                for (String key : whitelistNbt.getAllKeys()) {
                    tooltip.add(Component.literal("§8  - " + key + " : §5" + whitelistNbt.get(key)));
                }
            }

            if (!blacklistNbt.isEmpty()) {
                tooltip.add(Component.literal(" Blacklist :"));
                for (String key : blacklistNbt.getAllKeys()) {
                    tooltip.add(Component.literal("§8  - " + key + " : §5" + blacklistNbt.get(key)));
                }
            }
        }
    }
}
