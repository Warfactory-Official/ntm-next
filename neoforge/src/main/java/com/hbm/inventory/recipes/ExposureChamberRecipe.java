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

public class ExposureChamberRecipe extends GenericRecipe {

    public static final MapCodec<ExposureChamberRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(ExposureChamberRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, ExposureChamberRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(ExposureChamberRecipe::new);
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
        if (outputItems() == null || outputItems().length == 0) return ItemStack.EMPTY;
        return outputItems()[0].getRandom(GenericRecipes.RNG).orElse(ItemStack.EMPTY);
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return ExposureChamberRecipes.INSTANCE;
    }
}
