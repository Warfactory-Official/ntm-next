// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class SuperComputerRecipe extends GenericRecipe {
    public static final MapCodec<SuperComputerRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(SuperComputerRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, SuperComputerRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(SuperComputerRecipe::new);
    public static final RecipeSerializer<SuperComputerRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public SuperComputerRecipe(String name) {
        super(name);
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return SuperComputerRecipes.INSTANCE;
    }
}
