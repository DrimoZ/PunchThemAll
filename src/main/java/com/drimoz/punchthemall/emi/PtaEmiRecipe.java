package com.drimoz.punchthemall.emi;

import com.drimoz.punchthemall.PunchThemAll;
import com.drimoz.punchthemall.core.model.classes.PtaBlock;
import com.drimoz.punchthemall.core.model.classes.PtaConditions;
import com.drimoz.punchthemall.core.model.classes.PtaEffect;
import com.drimoz.punchthemall.core.model.classes.PtaExtras;
import com.drimoz.punchthemall.core.model.classes.PtaInteraction;
import com.drimoz.punchthemall.core.model.classes.PtaRewards;
import com.drimoz.punchthemall.core.model.classes.PtaTransformation;
import com.drimoz.punchthemall.core.model.records.PtaDropRecord;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** One EMI display per PunchThemAll interaction: inputs (hand + target) → outputs (drops + guaranteed + transformation). */
public class PtaEmiRecipe implements EmiRecipe {

    private final PtaInteraction interaction;
    private final ResourceLocation id;

    private final List<EmiIngredient> inputs = new ArrayList<>();
    private final List<EmiStack> outputSlots = new ArrayList<>();
    private final List<EmiStack> outputs = new ArrayList<>();
    private final int outputRows;

    public PtaEmiRecipe(PtaInteraction interaction) {
        this.interaction = interaction;
        this.id = ResourceLocation.fromNamespaceAndPath(PunchThemAll.MOD_ID,
                "interaction/" + interaction.getId().getNamespace() + "/" + interaction.getId().getPath());

        // Inputs: hand item(s) then target block/fluid.
        if (!interaction.getHand().isEmpty()) {
            inputs.add(ingredientOf(interaction.getHand().getStacks()));
        }
        PtaBlock block = interaction.getBlock();
        if (block.isBlock()) {
            inputs.add(ingredientOf(block.getBlockStacks()));
        } else if (block.isFluid()) {
            inputs.add(EmiStack.of(block.getFluid(), 1000));
        }

        // Outputs: weighted pool (with chance), then guaranteed, then transformation result.
        int totalWeight = interaction.getPool().getTotalPoolWeight();
        for (Map.Entry<PtaDropRecord, Integer> entry : interaction.getPool().getDropPool().entrySet()) {
            PtaDropRecord record = entry.getKey();
            if (record.isEmpty()) continue;
            float chance = totalWeight > 0 ? (float) entry.getValue() / totalWeight : 0f;
            EmiStack stack = EmiStack.of(sampleStack(record)).setChance(chance);
            outputSlots.add(stack);
            outputs.add(stack);
        }
        for (PtaDropRecord record : interaction.getRewards().getGuaranteed()) {
            if (record.isEmpty()) continue;
            EmiStack stack = EmiStack.of(sampleStack(record));
            outputSlots.add(stack);
            outputs.add(stack);
        }

        PtaTransformation transformation = interaction.getTransformation();
        if (!block.isAir() && transformation.hasTransformation() && !transformation.isAir()) {
            EmiStack result = EmiStack.of(transformationStack(transformation))
                    .setChance((float) transformation.getChance());
            outputs.add(result);
        }

        int count = outputSlots.size();
        this.outputRows = Math.max(1, (count + 8) / 9);
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return PtaEmiPlugin.CATEGORY;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return inputs;
    }

    @Override
    public List<EmiStack> getOutputs() {
        return outputs;
    }

    @Override
    public int getDisplayWidth() {
        return 162;
    }

    @Override
    public int getDisplayHeight() {
        return 32 + 18 * outputRows;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        int x = 0;
        int y = 4;

        for (EmiIngredient input : inputs) {
            widgets.addSlot(input, x, y);
            x += 18;
        }

        int arrowX = x + 2;
        widgets.addTexture(EmiTexture.EMPTY_ARROW, arrowX, y);
        int afterArrow = x + 28;

        PtaTransformation transformation = interaction.getTransformation();
        if (!interaction.getBlock().isAir() && transformation.hasTransformation() && !transformation.isAir()) {
            widgets.addSlot(EmiStack.of(transformationStack(transformation)), afterArrow, y);
        }

        // Hover the arrow for the rolls / Fortune / effects / conditions summary.
        List<Component> arrowInfo = buildArrowInfo();
        if (!arrowInfo.isEmpty()) {
            widgets.addTooltipText(arrowInfo, arrowX, y, 24, 17);
        }

        int oy = 32;
        int col = 0;
        int row = 0;
        for (EmiStack output : outputSlots) {
            widgets.addSlot(output, col * 18, oy + row * 18).recipeContext(this);
            col++;
            if (col == 9) {
                col = 0;
                row++;
            }
        }
    }

    // Helpers

    private List<Component> buildArrowInfo() {
        List<Component> lines = new ArrayList<>();
        PtaRewards rewards = interaction.getRewards();
        PtaExtras extras = interaction.getExtras();

        if (rewards.getRolls() != 1) lines.add(Component.literal("Rolls: " + rewards.getRolls()));
        if (rewards.hasFortune()) lines.add(Component.literal("Fortune bonus: x" + rewards.getFortuneFactor()));

        for (PtaEffect effect : extras.effects()) {
            String name = effect.effect().unwrapKey().map(k -> k.location().toString()).orElse("effect");
            lines.add(Component.literal("Effect: " + name + " " + (effect.amplifier() + 1) + " (" + (int) (effect.chance() * 100) + "%)"));
        }

        PtaConditions c = interaction.getConditions();
        if (!c.isEmpty()) {
            lines.add(Component.literal("Conditions:"));
            if (c.time() != PtaConditions.Time.ANY) lines.add(Component.literal(" - time: " + c.time().name().toLowerCase()));
            if (!c.weather().isEmpty()) lines.add(Component.literal(" - weather: " + c.weather()));
            if (c.yMin() != null || c.yMax() != null) lines.add(Component.literal(" - Y: " + c.yMin() + " to " + c.yMax()));
            if (c.lightMin() != null || c.lightMax() != null) lines.add(Component.literal(" - light: " + c.lightMin() + " to " + c.lightMax()));
            if (c.requiresSneaking() != null) lines.add(Component.literal(" - sneaking: " + c.requiresSneaking()));
            if (c.minFood() > 0) lines.add(Component.literal(" - min food: " + c.minFood()));
            if (c.minXpLevels() > 0) lines.add(Component.literal(" - min XP: " + c.minXpLevels()));
        }

        if (extras.hasSound()) lines.add(Component.literal("Plays a sound"));
        if (extras.hasParticles()) lines.add(Component.literal("Shows particles"));

        return lines;
    }

    private static EmiIngredient ingredientOf(List<ItemStack> stacks) {
        if (stacks.size() == 1) {
            return EmiStack.of(stacks.get(0));
        }
        List<EmiIngredient> list = new ArrayList<>();
        for (ItemStack stack : stacks) list.add(EmiStack.of(stack));
        return EmiIngredient.of(list);
    }

    private static ItemStack sampleStack(PtaDropRecord record) {
        Item item = record.items().stream().findFirst().orElse(null);
        return item == null ? ItemStack.EMPTY : new ItemStack(item, Math.max(1, record.max()));
    }

    private static ItemStack transformationStack(PtaTransformation transformation) {
        if (transformation.isBlock()) {
            return new ItemStack(transformation.getBlock());
        }
        return new ItemStack(transformation.getFluid().getBucket());
    }
}
