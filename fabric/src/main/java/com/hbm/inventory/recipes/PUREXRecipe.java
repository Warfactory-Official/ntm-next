// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class PUREXRecipe extends GenericRecipe {

    public static final MapCodec<PUREXRecipe> MAP_CODEC = GenericRecipe.mapCodec(PUREXRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, PUREXRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(PUREXRecipe::new);
    public static final RecipeSerializer<PUREXRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public PUREXRecipe(String name) {
        super(name);
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return PUREXRecipes.INSTANCE;
    }
}
