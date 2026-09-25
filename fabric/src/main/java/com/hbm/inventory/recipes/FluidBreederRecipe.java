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

public class FluidBreederRecipe extends GenericRecipe {

    public static final MapCodec<FluidBreederRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(FluidBreederRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, FluidBreederRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(FluidBreederRecipe::new);
    public static final RecipeSerializer<FluidBreederRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public FluidBreederRecipe(String name) {
        super(name);
    }

    public int cost() {
        return (int) inputFluid[0].amount();
    }

    public FluidStackNTM output() {
        return outputFluid[0];
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return FluidBreederRecipes.INSTANCE;
    }
}
