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
import com.hbm.inventory.recipes.loader.Outputs;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.random.Weighted;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class ShredderRecipe extends GenericRecipe {

    public static final MapCodec<ShredderRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(ShredderRecipe::new).validate(ShredderRecipe::validateData);
    public static final StreamCodec<RegistryFriendlyByteBuf, ShredderRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(ShredderRecipe::new);
    public static final RecipeSerializer<ShredderRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public ShredderRecipe(String name) {
        super(name);
    }

    private static DataResult<ShredderRecipe> validateData(ShredderRecipe recipe) {
        if (recipe.inputItem.length != 1 || recipe.inputItem[0].count() != 1) {
            return DataResult.error(() -> "shredder needs exactly one item per operation");
        }
        if (recipe.inputFluid.length != 0
                || recipe.outputFluid.length != 0
                || recipe.duration != 0
                || recipe.power != 0) {
            return DataResult.error(
                    () -> "shredder does not consume recipe fluids, duration or power");
        }
        var outputs = recipe.outputTemplates();
        List<Weighted<Optional<ItemStackTemplate>>> entries =
                outputs.size() == 1 ? outputs.getFirst().unwrap() : List.of();
        ItemStackTemplate identity = null;
        for (Weighted<Optional<ItemStackTemplate>> entry : entries) {
            if (entry.weight() <= 0)
                return DataResult.error(() -> "shredder output weights must be positive");
            if (entry.value().isEmpty()) continue;
            ItemStackTemplate stack = entry.value().get();
            if (identity != null
                    && (!identity.item().equals(stack.item())
                            || !identity.components().equals(stack.components()))) {
                return DataResult.error(
                        () -> "shredder output choices must share their item and components");
            }
            identity = stack;
        }
        if (identity == null)
            return DataResult.error(() -> "shredder needs one nonempty output slot");
        return DataResult.success(recipe);
    }

    public CountIngredient input() {
        return inputItem[0];
    }

    public ItemStack output() {
        if (outputItems() == null || outputItems().length == 0) return ItemStack.EMPTY;
        ItemStack maximum = Outputs.maximum(outputItems()[0]);
        return maximum == null ? ItemStack.EMPTY : maximum;
    }

    public ItemStack roll() {
        return outputItems()[0].getRandom(GenericRecipes.RNG).orElse(ItemStack.EMPTY).copy();
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return ShredderRecipes.INSTANCE;
    }
}
