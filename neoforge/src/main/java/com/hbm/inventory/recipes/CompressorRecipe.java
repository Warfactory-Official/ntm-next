// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class CompressorRecipe extends GenericRecipe {

    public static final MapCodec<CompressorRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(CompressorRecipe::new)
                    .validate(
                            row ->
                                    row.inputFluid.length == 0
                                            ? DataResult.error(
                                                    () -> "compressor needs an input fluid")
                                            : DataResult.success(row));
    public static final StreamCodec<RegistryFriendlyByteBuf, CompressorRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(CompressorRecipe::new);
    public static final RecipeSerializer<CompressorRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public CompressorRecipe(String name) {
        super(name);
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return CompressorRecipes.INSTANCE;
    }
}
