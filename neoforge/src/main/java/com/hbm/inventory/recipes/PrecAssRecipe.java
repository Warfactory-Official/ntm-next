// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class PrecAssRecipe extends GenericRecipe {

    public static final MapCodec<PrecAssRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(PrecAssRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, PrecAssRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(PrecAssRecipe::new);
    public static final RecipeSerializer<PrecAssRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public PrecAssRecipe(String name) {
        super(name);
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return PrecAssRecipes.INSTANCE;
    }
}
