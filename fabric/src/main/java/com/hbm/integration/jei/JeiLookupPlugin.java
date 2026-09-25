// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.jei;

import com.hbm.integration.recipeviewer.RecipePage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import mezz.jei.api.helpers.IPlatformFluidHelper;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.world.item.ItemStack;

final class JeiLookupPlugin<T> implements ISimpleRecipeManagerPlugin<T> {

    private final RecipePage<T> page;
    private final JeiPageCategory<T> category;
    private final RecipePage.LookupFilter filter;
    private final IIngredientManager ingredients;
    private final IPlatformFluidHelper<?> fluids;
    private Index<T> index = new Index<>(List.of(), List.of(), List.of());

    JeiLookupPlugin(
            RecipePage<T> page,
            JeiPageCategory<T> category,
            RecipePage.LookupFilter filter,
            IIngredientManager ingredients,
            IPlatformFluidHelper<?> fluids) {
        this.page = page;
        this.category = category;
        this.filter = filter;
        this.ingredients = ingredients;
        this.fluids = fluids;
    }

    IRecipeType<T> type() {
        return category.getRecipeType();
    }

    void fill() {
        List<T> rows = page.rows();
        List<Set<Object>> inputs = new ArrayList<>(rows.size());
        List<Set<Object>> outputs = new ArrayList<>(rows.size());
        for (T row : rows) {
            JeiPageCategory.RowIngredients slots = category.ingredients(row, ingredients, fluids);
            inputs.add(uids(slots.inputs()));
            outputs.add(uids(slots.outputs()));
        }
        index = new Index<>(rows, inputs, outputs);
    }

    private Set<Object> uids(List<ITypedIngredient<?>> slotIngredients) {
        Set<Object> uids = new HashSet<>();
        for (ITypedIngredient<?> ingredient : slotIngredients) uids.add(uid(ingredient));
        return uids;
    }

    private <V> Object uid(ITypedIngredient<V> ingredient) {
        return ingredients
                .getIngredientHelper(ingredient.getType())
                .getUid(ingredient, UidContext.Recipe);
    }

    private List<T> matching(boolean input, ITypedIngredient<?> looked, RecipePage.Lookup lookup) {
        ItemStack stack = looked.getItemStack().orElse(null);
        if (stack != null && !filter.answers(stack, lookup)) return List.of();
        Index<T> current = index;
        List<Set<Object>> sides = input ? current.inputs() : current.outputs();
        Object uid = uid(looked);
        List<T> found = new ArrayList<>();
        for (int i = 0; i < sides.size(); i++) {
            if (sides.get(i).contains(uid)) found.add(current.rows().get(i));
        }
        return found;
    }

    @Override
    public boolean isHandledInput(ITypedIngredient<?> input) {
        return !getRecipesForInput(input).isEmpty();
    }

    @Override
    public boolean isHandledOutput(ITypedIngredient<?> output) {
        return !getRecipesForOutput(output).isEmpty();
    }

    @Override
    public List<T> getRecipesForInput(ITypedIngredient<?> input) {
        return matching(true, input, RecipePage.Lookup.USES);
    }

    @Override
    public List<T> getRecipesForOutput(ITypedIngredient<?> output) {
        return matching(false, output, RecipePage.Lookup.RECIPES);
    }

    @Override
    public List<T> getAllRecipes() {
        return index.rows();
    }

    private record Index<T>(List<T> rows, List<Set<Object>> inputs, List<Set<Object>> outputs) {}
}
