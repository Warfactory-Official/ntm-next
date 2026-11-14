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
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class ExposureChamberRecipe extends GenericRecipe {

    public static final MapCodec<ExposureChamberRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(ExposureChamberRecipe::new)
                    .validate(ExposureChamberRecipe::validated);
    public static final StreamCodec<RegistryFriendlyByteBuf, ExposureChamberRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(ExposureChamberRecipe::new)
                    .map(ExposureChamberRecipe::checked, ExposureChamberRecipe::checked);
    public static final RecipeSerializer<ExposureChamberRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public ExposureChamberRecipe(String name) {
        super(name);
    }

    public CountIngredient particle() {
        return inputItem[0];
    }

    public CountIngredient ingredient() {
        return inputItem[1];
    }

    public ItemStack output() {
        return outputItems()[0].getRandom(GenericRecipes.RNG).orElse(ItemStack.EMPTY);
    }

    private static ExposureChamberRecipe checked(ExposureChamberRecipe recipe) {
        return validated(recipe).getOrThrow();
    }

    private static DataResult<ExposureChamberRecipe> validated(ExposureChamberRecipe recipe) {
        if (recipe.inputItem.length < 2 || recipe.outputTemplates().isEmpty()) {
            return DataResult.error(() -> "exposure requires a particle, material and product");
        }
        return DataResult.success(recipe);
    }

    @Override
    public void materialize() {
        checked(this);
        super.materialize();
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return ExposureChamberRecipes.INSTANCE;
    }
}
