package com.drimoz.punchthemall.emi;

import com.drimoz.punchthemall.PunchThemAll;
import com.drimoz.punchthemall.core.model.classes.PtaInteraction;
import com.drimoz.punchthemall.core.registry.InteractionRegistry;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

/**
 * Native EMI integration. Like JEI, the interaction registry is empty when EMI first loads and fills
 * once a world's data is available; EMI re-runs {@link #register} whenever recipes reload (after the
 * datapack registry syncs), so the category reflects the server's interactions.
 */
@EmiEntrypoint
public class PtaEmiPlugin implements EmiPlugin {

    public static final EmiRecipeCategory CATEGORY = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(PunchThemAll.MOD_ID, "interactions"),
            EmiStack.of(Items.WOODEN_PICKAXE)
    );

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(CATEGORY);
        for (PtaInteraction interaction : InteractionRegistry.getInstance().getInteractions().values()) {
            registry.addRecipe(new PtaEmiRecipe(interaction));
        }
    }
}
