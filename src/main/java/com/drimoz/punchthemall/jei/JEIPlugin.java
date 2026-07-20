package com.drimoz.punchthemall.jei;

import com.drimoz.punchthemall.PunchThemAll;
import com.drimoz.punchthemall.core.model.classes.PtaInteraction;
import com.drimoz.punchthemall.core.registry.InteractionRegistry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI 19 integration. Because interactions are datapack data, the registry is empty when JEI loads
 * and only fills once a world is loaded (server data reload). So recipes are pushed at runtime — on
 * {@link #onRuntimeAvailable} and whenever the client's recipes update ({@link RecipesUpdatedEvent}) —
 * rather than in {@code registerRecipes}. This keeps JEI in sync with the server's interactions.
 */
@JeiPlugin
public class JEIPlugin implements IModPlugin {

    public static final RecipeType<PtaInteraction> INTERACTION_RECIPE_TYPE =
            RecipeType.create(PunchThemAll.MOD_ID, "interactions", PtaInteraction.class);

    private static IJeiRuntime runtime;
    private static final List<PtaInteraction> SHOWN = new ArrayList<>();

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(PunchThemAll.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        final IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new JeiCategory(guiHelper));
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        // Data updates are pushed by PtaClientEvents (RecipesUpdatedEvent -> refreshFromRegistry).
        applyRegistryToRuntime();
    }

    /** Re-populate JEI from the current (server-synchronised) interaction registry. */
    public static void refreshFromRegistry() {
        applyRegistryToRuntime();
    }

    private static synchronized void applyRegistryToRuntime() {
        if (runtime == null) return;

        if (!SHOWN.isEmpty()) {
            runtime.getRecipeManager().hideRecipes(INTERACTION_RECIPE_TYPE, new ArrayList<>(SHOWN));
            SHOWN.clear();
        }

        List<PtaInteraction> current = new ArrayList<>(InteractionRegistry.getInstance().getInteractions().values());
        if (!current.isEmpty()) {
            runtime.getRecipeManager().addRecipes(INTERACTION_RECIPE_TYPE, current);
            SHOWN.addAll(current);
        }
    }
}
