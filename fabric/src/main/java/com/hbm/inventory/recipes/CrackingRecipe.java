// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class CrackingRecipe extends GenericRecipe {

    public static final MapCodec<CrackingRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(CrackingRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, CrackingRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(CrackingRecipe::new);
    public static final RecipeSerializer<CrackingRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public CrackingRecipe(String name) {
        super(name);
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return CrackingRecipes.INSTANCE;
    }
}
