// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class LiquefactionRecipe extends GenericRecipe {

    public static final MapCodec<LiquefactionRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(LiquefactionRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, LiquefactionRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(LiquefactionRecipe::new);
    public static final RecipeSerializer<LiquefactionRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public LiquefactionRecipe(String name) {
        super(name);
    }

    public FluidStackNTM melt() {
        return outputFluid[0];
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return LiquefactionRecipes.INSTANCE;
    }
}
