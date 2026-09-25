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

public class SolidificationRecipe extends GenericRecipe {

    public static final MapCodec<SolidificationRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(SolidificationRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, SolidificationRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(SolidificationRecipe::new);
    public static final RecipeSerializer<SolidificationRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public SolidificationRecipe(String name) {
        super(name);
    }

    public int fillReq() {
        return (int) inputFluid[0].amount();
    }

    public ItemStack output() {
        return outputItems().length > 0
                ? outputItems()[0].getRandom(GenericRecipes.RNG).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return SolidificationRecipes.INSTANCE;
    }
}
