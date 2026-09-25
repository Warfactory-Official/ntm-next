// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.inventory.recipes.loader.RecipeIndex;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class CompressorRecipes
        extends GenericRecipes<
                CompressorRecipe, RecipeIndex<Pair<Fluid, Integer>, CompressorRecipe>> {

    public static final CompressorRecipes INSTANCE = new CompressorRecipes();

    @Override
    protected String registryName() {
        return "compressor";
    }

    @Override
    protected RecipeSerializer<CompressorRecipe> serializer() {
        return CompressorRecipe.SERIALIZER;
    }

    @Override
    protected RecipeIndex<Pair<Fluid, Integer>, CompressorRecipe> indexRows(
            List<CompressorRecipe> rows) {
        return RecipeIndex.of(
                rows,
                recipe -> Pair.of(recipe.inputFluid[0].type(), recipe.inputFluid[0].pressure()));
    }

    public @Nullable CompressorRecipe get(@Nullable Fluid type, int pressure) {
        if (type == null) return null;
        return index().findLast(Pair.of(type, pressure));
    }
}
