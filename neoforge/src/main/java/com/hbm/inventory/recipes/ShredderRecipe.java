// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
        if (outputs.size() != 1
                || outputs.getFirst().unwrap().size() != 1
                || outputs.getFirst().unwrap().getFirst().weight() <= 0) {
            return DataResult.error(
                    () -> "shredder needs one fixed output, without chance or alternatives");
        }
        return DataResult.success(recipe);
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
        return ShredderRecipes.INSTANCE;
    }
}
