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
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jspecify.annotations.Nullable;

public class MixerRecipe extends GenericRecipe {

    public static final MapCodec<MixerRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(MixerRecipe::new)
                    .validate(
                            row ->
                                    row.outputFluid.length == 0
                                            ? DataResult.error(() -> "mixer needs an output fluid")
                                            : DataResult.success(row));
    public static final StreamCodec<RegistryFriendlyByteBuf, MixerRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(MixerRecipe::new);
    public static final RecipeSerializer<MixerRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public MixerRecipe(String name) {
        super(name);
    }

    public @Nullable FluidStackNTM input1() {
        return inputFluid.length > 0 ? inputFluid[0] : null;
    }

    public @Nullable FluidStackNTM input2() {
        return inputFluid.length > 1 ? inputFluid[1] : null;
    }

    public @Nullable CountIngredient solidInput() {
        return inputItem == null || inputItem.length == 0 ? null : inputItem[0];
    }

    public int output() {
        return (int) outputFluid[0].amount();
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return MixerRecipes.INSTANCE;
    }
}
