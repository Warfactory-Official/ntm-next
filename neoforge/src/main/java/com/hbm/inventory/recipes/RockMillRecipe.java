// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class RockMillRecipe extends GenericRecipe {
    public static final MapCodec<RockMillRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(RockMillRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, RockMillRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(RockMillRecipe::new);
    public static final RecipeSerializer<RockMillRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public RockMillRecipe(String name) {
        super(name);
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return RockMillRecipes.INSTANCE;
    }
}
