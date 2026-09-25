// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class MagicRecipe extends GenericRecipe {

    public static final MapCodec<MagicRecipe> MAP_CODEC = GenericRecipe.mapCodec(MagicRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, MagicRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(MagicRecipe::new);
    public static final RecipeSerializer<MagicRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public MagicRecipe(String name) {
        super(name);
    }

    public boolean matchesContents(List<ItemStack> contents) {
        if (contents.size() != inputItem.length) return false;
        for (int i = 0; i < inputItem.length; i++) {
            if (!inputItem[i].matchesItem(contents.get(i))) return false;
        }
        return true;
    }

    public ItemStack result() {
        return outputItems()[0].getRandom(GenericRecipes.RNG).orElse(ItemStack.EMPTY);
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return MagicRecipes.INSTANCE;
    }
}
