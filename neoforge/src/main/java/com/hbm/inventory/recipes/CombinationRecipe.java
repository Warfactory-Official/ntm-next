// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class CombinationRecipe extends GenericRecipe {

    public static final MapCodec<CombinationRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(CombinationRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, CombinationRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(CombinationRecipe::new);
    public static final RecipeSerializer<CombinationRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public CombinationRecipe(String name) {
        super(name);
    }

    public ItemStack makeOutput() {
        if (outputItems() == null || outputItems().length == 0) return ItemStack.EMPTY;
        return outputItems()[0].getRandom(GenericRecipes.RNG).orElse(ItemStack.EMPTY).copy();
    }

    public @Nullable Fluid fluidType() {
        return outputFluid.length == 0 ? null : outputFluid[0].type();
    }

    public int fluidAmount() {
        return outputFluid.length == 0 ? 0 : (int) outputFluid[0].amount();
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return CombinationRecipes.INSTANCE;
    }
}
