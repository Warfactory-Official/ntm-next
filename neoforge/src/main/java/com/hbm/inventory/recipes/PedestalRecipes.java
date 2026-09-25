// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.GenericRecipes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class PedestalRecipes extends GenericRecipes<PedestalRecipe, PedestalRecipes.Index> {

    public static final PedestalRecipes INSTANCE = new PedestalRecipes();

    public static final int SET_COUNT = 2;

    public static List<PedestalRecipe> recipeSet(int variant) {
        return INSTANCE.index().sets().get(Math.abs(variant) % SET_COUNT);
    }

    @Override
    protected String registryName() {
        return "pedestal";
    }

    @Override
    protected RecipeSerializer<PedestalRecipe> serializer() {
        return PedestalRecipe.SERIALIZER;
    }

    @Override
    protected Index indexRows(List<PedestalRecipe> rows) {
        List<List<PedestalRecipe>> sets = new ArrayList<>(SET_COUNT);
        for (int i = 0; i < SET_COUNT; i++) sets.add(new ArrayList<>());
        for (PedestalRecipe recipe : rows) {
            sets.get(Math.abs(recipe.recipeSet) % SET_COUNT).add(recipe);
        }
        return new Index(sets.stream().map(List::copyOf).toList());
    }

    public enum ExtraCondition {
        NONE,
        FULL_MOON,
        NEW_MOON,
        SUN,
        GOOD_KARMA,
        BAD_KARMA
    }

    record Index(List<List<PedestalRecipe>> sets) {}
}
