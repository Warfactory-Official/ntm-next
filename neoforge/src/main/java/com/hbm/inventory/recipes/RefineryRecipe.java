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

public class RefineryRecipe extends GenericRecipe {

    public static final MapCodec<RefineryRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(RefineryRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, RefineryRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(RefineryRecipe::new);
    public static final RecipeSerializer<RefineryRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public RefineryRecipe(String name) {
        super(name);
    }

    public ItemStack solidOutput() {
        return outputItems().length > 0
                ? outputItems()[0].getRandom(GenericRecipes.RNG).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return RefineryRecipes.INSTANCE;
    }
}
