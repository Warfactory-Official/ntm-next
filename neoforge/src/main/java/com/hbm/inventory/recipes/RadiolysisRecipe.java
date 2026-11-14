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
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class RadiolysisRecipe extends GenericRecipe {

    public static final MapCodec<RadiolysisRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(RadiolysisRecipe::new)
                    .validate(
                            recipe ->
                                    validateProducts(recipe.outputFluid)
                                            .map(_ -> recipe));
    public static final StreamCodec<RegistryFriendlyByteBuf, RadiolysisRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(RadiolysisRecipe::new)
                    .map(RadiolysisRecipe::checked, RadiolysisRecipe::checked);
    public static final RecipeSerializer<RadiolysisRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public RadiolysisRecipe(String name) {
        super(name);
    }

    private static RadiolysisRecipe checked(RadiolysisRecipe recipe) {
        validateProducts(recipe.outputFluid).getOrThrow();
        return recipe;
    }

    private static DataResult<FluidStackNTM[]> validateProducts(FluidStackNTM[] products) {
        return products.length < 2
                ? DataResult.error(() -> "radiolysis requires two product descriptors")
                : DataResult.success(products);
    }

    @Override
    public RadiolysisRecipe outputFluids(FluidStackNTM... products) {
        validateProducts(products).getOrThrow();
        super.outputFluids(products);
        return this;
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return RadiolysisRecipes.INSTANCE;
    }
}
