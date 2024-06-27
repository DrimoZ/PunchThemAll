package com.drimoz.punchthemall.jei;

import com.drimoz.punchthemall.PunchThemAll;
import com.drimoz.punchthemall.core.model.Interaction;
import com.drimoz.punchthemall.core.registry.InteractionRegistry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class JEIPlugin implements IModPlugin {
    public static final RecipeType<Interaction> INTERACTION_RECIPE_TYPE = RecipeType.create(PunchThemAll.MOD_ID, "air_interactions", Interaction.class);


    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(PunchThemAll.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        final IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();

        registration.addRecipeCategories(new JeiCategory(guiHelper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(INTERACTION_RECIPE_TYPE, InteractionRegistry.getInstance().getInteractions().values().stream().toList());
    }
}
