// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class FuelPoolRecipe extends GenericRecipe {

    public static final MapCodec<FuelPoolRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(FuelPoolRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, FuelPoolRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(FuelPoolRecipe::new);
    public static final RecipeSerializer<FuelPoolRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public FuelPoolRecipe(String name) {
        super(name);
    }

    public CountIngredient input() {
        return inputItem[0];
    }

    public ItemStack output() {
        return outputItems() == null || outputItems().length == 0
                ? ItemStack.EMPTY
                : outputItems()[0].unwrap().getFirst().value();
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return FuelPoolRecipes.INSTANCE;
    }
}
