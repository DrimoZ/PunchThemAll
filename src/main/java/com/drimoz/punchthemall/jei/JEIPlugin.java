package com.drimoz.punchthemall.jei;

import com.drimoz.punchthemall.PunchThemAll;
import com.drimoz.punchthemall.core.model.classes.PtaInteraction;
import com.drimoz.punchthemall.core.registry.InteractionRegistry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class JEIPlugin implements IModPlugin {
    public static final RecipeType<PtaInteraction> INTERACTION_RECIPE_TYPE = RecipeType.create(PunchThemAll.MOD_ID, "air_interactions", PtaInteraction.class);

    // Captured JEI runtime + the recipes we pushed at runtime, so we can replace them when the
    // server re-synchronises the registry (dedicated servers, /reload).
    private static IJeiRuntime runtime;
    private static final List<PtaInteraction> DYNAMIC_RECIPES = new ArrayList<>();


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

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        // If a server sync landed before the runtime was ready, reflect it now.
        applyRegistryToRuntime();
    }

    /**
     * Re-populate JEI from the current (server-synchronised) registry. Called after an S2C sync.
     *
     * <p>No-op in single-player / on the host: there the registry is shared with the integrated
     * server and {@link #registerRecipes} already added everything, so touching the runtime would
     * duplicate entries. On a dedicated-server client the registry is empty at JEI load, so this is
     * the only path that shows PunchThemAll recipes.</p>
     */
    public static void refreshFromRegistry() {
        applyRegistryToRuntime();
    }

    private static synchronized void applyRegistryToRuntime() {
        if (runtime == null) return;
        if (Minecraft.getInstance().getSingleplayerServer() != null) return;

        if (!DYNAMIC_RECIPES.isEmpty()) {
            runtime.getRecipeManager().hideRecipes(INTERACTION_RECIPE_TYPE, new ArrayList<>(DYNAMIC_RECIPES));
            DYNAMIC_RECIPES.clear();
        }

        List<PtaInteraction> current = new ArrayList<>(InteractionRegistry.getInstance().getInteractions().values());
        if (!current.isEmpty()) {
            runtime.getRecipeManager().addRecipes(INTERACTION_RECIPE_TYPE, current);
            DYNAMIC_RECIPES.addAll(current);
        }
    }
}
