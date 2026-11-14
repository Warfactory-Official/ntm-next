/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.inventory.recipes.loader.RecipeIndex;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jspecify.annotations.Nullable;

public class ExposureChamberRecipes
        extends GenericRecipes<ExposureChamberRecipe, RecipeIndex<Item, ExposureChamberRecipe>> {

    public static final ExposureChamberRecipes INSTANCE = new ExposureChamberRecipes();

    public static @Nullable ExposureChamberRecipe getRecipe(
            ItemStack particle, ItemStack ingredient) {
        if (particle.isEmpty() || ingredient.isEmpty()) return null;
        var candidates = INSTANCE.byInputItem().candidates(particle.getItem());
        for (int i = 0; i < candidates.size(); i++) {
            var recipe = candidates.get(i);
            if (recipe.particle().matchesItem(particle)
                    && recipe.ingredient().matchesItem(ingredient)) return recipe;
        }
        return null;
    }

    public static boolean anyRowNames(ItemStack stack, boolean asParticle) {
        var indexed = asParticle ? INSTANCE.byInputItem() : INSTANCE.index();
        var candidates = indexed.candidates(stack.getItem());
        for (int i = 0; i < candidates.size(); i++) {
            var recipe = candidates.get(i);
            if ((asParticle ? recipe.particle() : recipe.ingredient()).matchesItem(stack))
                return true;
        }
        return false;
    }

    @Override
    protected RecipeIndex<Item, ExposureChamberRecipe> indexRows(List<ExposureChamberRecipe> rows) {
        return RecipeIndex.of(
                rows,
                (recipe, file) ->
                        recipe.ingredient()
                                .ingredient()
                                .items()
                                .forEach(item -> file.accept(item.value())));
    }

    @Override
    protected String registryName() {
        return "exposure_chamber";
    }

    @Override
    protected RecipeSerializer<ExposureChamberRecipe> serializer() {
        return ExposureChamberRecipe.SERIALIZER;
    }
}
