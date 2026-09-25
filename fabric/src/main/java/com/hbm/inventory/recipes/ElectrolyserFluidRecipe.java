// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class ElectrolyserFluidRecipe extends GenericRecipe {

    public static final MapCodec<ElectrolyserFluidRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(ElectrolyserFluidRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, ElectrolyserFluidRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(ElectrolyserFluidRecipe::new);
    public static final RecipeSerializer<ElectrolyserFluidRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public ElectrolyserFluidRecipe(String name) {
        super(name);
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return ElectrolyserFluidRecipes.INSTANCE;
    }
}
