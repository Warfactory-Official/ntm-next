// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.jei;

import com.hbm.client.gui.ScreenAnvil;
import com.hbm.client.gui.ScreenMachineAutocrafter;
import com.hbm.client.gui.ScreenMachineCustom;
import com.hbm.integration.recipeviewer.CraftingRows;
import com.hbm.integration.recipeviewer.CustomMachinePage;
import com.hbm.integration.recipeviewer.RecipePage;
import com.hbm.integration.recipeviewer.RecipePages;
import com.hbm.inventory.machine.CustomMachineDefinition;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.lib.Library;
import com.hbm.registration.Reg;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.*;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jspecify.annotations.NonNull;

@JeiPlugin
public class NTMJeiPlugin implements IModPlugin {
    private final Map<ResourceKey<CustomMachineDefinition>, CustomMachinePage> customMachines =
            new LinkedHashMap<>();
    private final Map<Identifier, JeiLookupPlugin<?>> lookupPlugins = new HashMap<>();
    private final IdentifierCraftingLookup identifierLookup = new IdentifierCraftingLookup();

    private static <T> IRecipeType<T> typeOf(RecipePage<T> page) {
        return IRecipeType.create(page.id(), page.rowType());
    }

    private List<RecipePage<?>> pages() {
        List<RecipePage<?>> pages = new ArrayList<>(RecipePages.fixed());
        pages.addAll(customMachines.values());
        return pages;
    }

    @Override
    public Identifier getPluginUid() {
        return Library.id("jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var helpers = registration.getJeiHelpers();
        long unitsPerMilliBucket = helpers.getPlatformFluidHelper().bucketVolume() / 1000L;
        customMachines.clear();
        for (CustomMachinePage page : RecipePages.customMachines()) {
            customMachines.put(page.key(), page);
        }
        lookupPlugins.clear();
        for (RecipePage<?> page : pages())
            register(registration, page, helpers, unitsPerMilliBucket);
    }

    private <T> void register(
            IRecipeCategoryRegistration registration,
            RecipePage<T> page,
            IJeiHelpers helpers,
            long unitsPerMilliBucket) {
        JeiPageCategory<T> category =
                new JeiPageCategory<>(
                        page, typeOf(page), helpers.getGuiHelper(), unitsPerMilliBucket);
        registration.addRecipeCategories(category);
        RecipePage.LookupFilter filter = page.lookupFilter();
        if (filter != null) {
            lookupPlugins.put(
                    page.id(),
                    new JeiLookupPlugin<>(
                            page,
                            category,
                            filter,
                            helpers.getIngredientManager(),
                            helpers.getPlatformFluidHelper()));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (RecipePage<?> page : pages()) addRows(registration, page);
    }

    private <T> void addRows(IRecipeRegistration registration, RecipePage<T> page) {
        JeiLookupPlugin<?> plugin = lookupPlugins.get(page.id());
        if (plugin == null) registration.addRecipes(typeOf(page), page.rows());
        else plugin.fill();
    }

    @Override
    public void registerAdvanced(IAdvancedRegistration registration) {
        for (JeiLookupPlugin<?> plugin : lookupPlugins.values())
            addLookupPlugin(registration, plugin);
        registration.addSimpleRecipeManagerPlugin(RecipeTypes.CRAFTING, identifierLookup);
        registration.addSimpleRecipeManagerPlugin(
                RecipeTypes.CRAFTING, new CraftingRowsLookup(new CraftingRows()));
    }

    @Override
    public void registerExtraIngredients(IExtraIngredientRegistration registration) {
        registration.addExtraItemStacks(List.of(new ItemStack(ModItems.FLUID_IDENTIFIER)));
    }

    private static <T> void addLookupPlugin(
            IAdvancedRegistration registration, JeiLookupPlugin<T> plugin) {
        registration.addSimpleRecipeManagerPlugin(plugin.type(), plugin);
    }

    @Override
    public void registerItemSubtypes(@NonNull ISubtypeRegistration registration) {
        Reg.visitItemSubtypes(
                (item, kind) ->
                        registration.registerSubtypeInterpreter(
                                item, (stack, context) -> kind.getSubtypeData(stack)));
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        identifierLookup.recipe =
                jeiRuntime
                        .getRecipeManager()
                        .createRecipeLookup(RecipeTypes.CRAFTING)
                        .includeHidden()
                        .get()
                        .filter(
                                row ->
                                        row.id()
                                                .identifier()
                                                .equals(Library.id("fluid_identifier_multi")))
                        .findFirst()
                        .orElse(null);
        jeiRuntime
                .getIngredientManager()
                .removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, RecipePages.hiddenStacks());
        for (RecipePage<?> page : RecipePages.fixed()) hideRows(jeiRuntime, page);
    }

    @Override
    public void onRuntimeUnavailable() {
        identifierLookup.recipe = null;
    }

    private static final class IdentifierCraftingLookup
            implements ISimpleRecipeManagerPlugin<RecipeHolder<CraftingRecipe>> {
        private volatile RecipeHolder<CraftingRecipe> recipe;

        @Override
        public boolean isHandledInput(ITypedIngredient<?> input) {
            return false;
        }

        @Override
        public boolean isHandledOutput(ITypedIngredient<?> output) {
            return !getRecipesForOutput(output).isEmpty();
        }

        @Override
        public List<RecipeHolder<CraftingRecipe>> getRecipesForInput(ITypedIngredient<?> input) {
            return List.of();
        }

        @Override
        public List<RecipeHolder<CraftingRecipe>> getRecipesForOutput(ITypedIngredient<?> output) {
            ItemStack stack = output.getItemStack().orElse(ItemStack.EMPTY);
            if (!stack.is(ModItems.FLUID_IDENTIFIER.get())
                    || stack.getOrDefault(
                                    ModDataComponents.FLUID_IDENTIFIER.get(),
                                    FluidIdentifierData.EMPTY)
                            .equals(FluidIdentifierData.EMPTY)) {
                return List.of();
            }
            RecipeHolder<CraftingRecipe> current = recipe;
            return current == null ? List.of() : List.of(current);
        }

        @Override
        public List<RecipeHolder<CraftingRecipe>> getAllRecipes() {
            return List.of();
        }
    }

    private record CraftingRowsLookup(CraftingRows rows)
            implements ISimpleRecipeManagerPlugin<RecipeHolder<CraftingRecipe>> {
        @Override
        public boolean isHandledInput(ITypedIngredient<?> input) {
            return !getRecipesForInput(input).isEmpty();
        }

        @Override
        public boolean isHandledOutput(ITypedIngredient<?> output) {
            return !getRecipesForOutput(output).isEmpty();
        }

        @Override
        public List<RecipeHolder<CraftingRecipe>> getRecipesForInput(ITypedIngredient<?> input) {
            return input.getItemStack().map(rows::usesOf).orElse(List.of());
        }

        @Override
        public List<RecipeHolder<CraftingRecipe>> getRecipesForOutput(ITypedIngredient<?> output) {
            return output.getItemStack().map(rows::recipesFor).orElse(List.of());
        }

        @Override
        public List<RecipeHolder<CraftingRecipe>> getAllRecipes() {
            return List.of();
        }
    }

    private <T> void hideRows(IJeiRuntime runtime, RecipePage<T> page) {
        IRecipeType<T> type = typeOf(page);
        List<T> hidden =
                runtime.getRecipeManager()
                        .createRecipeLookup(type)
                        .includeHidden()
                        .get()
                        .filter(page::hidden)
                        .toList();
        if (!hidden.isEmpty()) runtime.getRecipeManager().hideRecipes(type, hidden);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (RecipePage<?> page : pages()) {
            registration.addCraftingStation(
                    typeOf(page), page.catalysts().toArray(ItemStack[]::new));
        }
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(
                ScreenAnvil.class,
                new IGuiContainerHandler<ScreenAnvil>() {
                    @Override
                    public List<Rect2i> getGuiExtraAreas(ScreenAnvil screen) {
                        return List.of(screen.recipePanelArea());
                    }
                });
        Map<Identifier, RecipePage<?>> pages = new HashMap<>();
        for (RecipePage<?> page : RecipePages.fixed()) pages.put(page.id(), page);
        for (RecipePages.ClickArea area : RecipePages.clickAreas()) {
            IRecipeType<?> type =
                    area.page().equals(RecipePages.VANILLA_SMELTING)
                            ? RecipeTypes.SMELTING
                            : typeOf(pages.get(area.page()));
            registration.addRecipeClickArea(
                    area.screen(), area.x(), area.y(), area.width(), area.height(), type);
        }
        registration.addGuiContainerHandler(
                ScreenMachineCustom.class,
                new IGuiContainerHandler<ScreenMachineCustom>() {
                    @Override
                    public Collection<IGuiClickableArea> getGuiClickableAreas(
                            ScreenMachineCustom screen, double mouseX, double mouseY) {
                        CustomMachinePage page =
                                customMachines.get(screen.getMenu().blockEntity().machineType);
                        return page == null
                                ? List.of()
                                : List.of(
                                        IGuiClickableArea.createBasic(
                                                RecipePages.CUSTOM_MACHINE_X,
                                                RecipePages.CUSTOM_MACHINE_Y,
                                                RecipePages.CUSTOM_MACHINE_WIDTH,
                                                RecipePages.CUSTOM_MACHINE_HEIGHT,
                                                typeOf(page)));
                    }
                });
        registration.addGhostIngredientHandler(
                ScreenMachineAutocrafter.class, new AutocrafterGhostHandler());
    }
}
