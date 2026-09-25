// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class VacuumRefineryRecipe extends GenericRecipe {

    public static final MapCodec<VacuumRefineryRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(VacuumRefineryRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, VacuumRefineryRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(VacuumRefineryRecipe::new);
    public static final RecipeSerializer<VacuumRefineryRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public VacuumRefineryRecipe(String name) {
        super(name);
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return VacuumRefineryRecipes.INSTANCE;
    }
}
