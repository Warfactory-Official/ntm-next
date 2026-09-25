// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jspecify.annotations.Nullable;

public class PyroOvenRecipe extends GenericRecipe {

    public static final MapCodec<PyroOvenRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(PyroOvenRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, PyroOvenRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(PyroOvenRecipe::new);
    public static final RecipeSerializer<PyroOvenRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public PyroOvenRecipe(String name) {
        super(name);
    }

    public @Nullable FluidStackNTM inFluid() {
        return inputFluid.length > 0 ? inputFluid[0] : null;
    }

    public @Nullable CountIngredient inItem() {
        return inputItem.length > 0 ? inputItem[0] : null;
    }

    public @Nullable FluidStackNTM outFluid() {
        return outputFluid.length > 0 ? outputFluid[0] : null;
    }

    public ItemStack outItem() {
        return outputItems().length > 0
                ? outputItems()[0].getRandom(GenericRecipes.RNG).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return PyroOvenRecipes.INSTANCE;
    }
}
