// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class LemegetonRecipe extends GenericRecipe {

    public static final MapCodec<LemegetonRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(LemegetonRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, LemegetonRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(LemegetonRecipe::new);
    public static final RecipeSerializer<LemegetonRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public LemegetonRecipe(String name) {
        super(name);
    }

    public boolean accepts(ItemStack stack) {
        return inputItem[0].test(stack);
    }

    public ItemStack result() {
        return outputItems()[0].getRandom(GenericRecipes.RNG).orElse(ItemStack.EMPTY).copy();
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return LemegetonRecipes.INSTANCE;
    }
}
