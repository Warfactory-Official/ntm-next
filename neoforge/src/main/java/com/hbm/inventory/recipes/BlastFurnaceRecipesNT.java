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

import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jspecify.annotations.Nullable;

public class BlastFurnaceRecipesNT extends SimpleGenericRecipes<BlastFurnaceRecipe> {

    public static final BlastFurnaceRecipesNT INSTANCE = new BlastFurnaceRecipesNT();

    @Override
    protected String registryName() {
        return "blast_furnace";
    }

    @Override
    protected RecipeSerializer<BlastFurnaceRecipe> serializer() {
        return BlastFurnaceRecipe.SERIALIZER;
    }

    public @Nullable BlastFurnaceRecipe getRecipe(ItemStack s0, ItemStack s1) {
        for (BlastFurnaceRecipe recipe : recipes()) {
            CountIngredient[] in = recipe.inputItem;
            if (in.length == 1) {
                if (!s0.isEmpty() && s1.isEmpty() && in[0].matchesItem(s0)) return recipe;
                if (s0.isEmpty() && !s1.isEmpty() && in[0].matchesItem(s1)) return recipe;
            } else if (in.length == 2 && !s0.isEmpty() && !s1.isEmpty()) {
                if (in[0].matchesItem(s0) && in[1].matchesItem(s1)) return recipe;
                if (in[1].matchesItem(s0) && in[0].matchesItem(s1)) return recipe;
            }
        }
        return null;
    }
}
