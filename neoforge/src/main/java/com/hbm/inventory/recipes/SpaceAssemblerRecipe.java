// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class SpaceAssemblerRecipe extends GenericRecipe {

    public static final MapCodec<SpaceAssemblerRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(SpaceAssemblerRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, SpaceAssemblerRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(SpaceAssemblerRecipe::new);
    public static final RecipeSerializer<SpaceAssemblerRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public SpaceAssemblerRecipe(String name) {
        super(name);
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return SpaceAssemblerRecipes.INSTANCE;
    }
}
