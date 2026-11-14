/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jspecify.annotations.Nullable;

public class OutgasserRecipe extends GenericRecipe {

    public static final MapCodec<OutgasserRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(
                    OutgasserRecipe::new,
                    Codec.BOOL.optionalFieldOf("fusion_only", false),
                    (recipe, fusionOnly) -> recipe.fusionOnly = fusionOnly,
                    recipe -> recipe.fusionOnly);
    private static final StreamCodec<RegistryFriendlyByteBuf, Boolean> FUSION_ONLY_STREAM_CODEC =
            ByteBufCodecs.BOOL.cast();
    public static final StreamCodec<RegistryFriendlyByteBuf, OutgasserRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(
                    OutgasserRecipe::new,
                    FUSION_ONLY_STREAM_CODEC,
                    (recipe, fusionOnly) -> recipe.fusionOnly = fusionOnly,
                    recipe -> recipe.fusionOnly);
    public static final RecipeSerializer<OutgasserRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
    public boolean fusionOnly = false;

    public OutgasserRecipe(String name) {
        super(name);
    }

    public OutgasserRecipe fusionOnly() {
        this.fusionOnly = true;
        return this;
    }

    public @Nullable FluidStackNTM fluidOutput() {
        return outputFluid.length > 0 ? outputFluid[0] : null;
    }

    public ItemStack solidOutput() {
        return outputItems().length > 0
                ? outputItems()[0].getRandom(GenericRecipes.RNG).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return OutgasserRecipes.INSTANCE;
    }
}
