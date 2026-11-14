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

import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class CompressorRecipes
        extends GenericRecipes<CompressorRecipe, Map<Fluid, CompressorRecipe[]>> {

    public static final CompressorRecipes INSTANCE = new CompressorRecipes();

    @Override
    protected String registryName() {
        return "compressor";
    }

    @Override
    protected RecipeSerializer<CompressorRecipe> serializer() {
        return CompressorRecipe.SERIALIZER;
    }

    @Override
    protected Map<Fluid, CompressorRecipe[]> indexRows(List<CompressorRecipe> rows) {
        var indexed = new HashMap<Fluid, CompressorRecipe[]>();
        for (var recipe : rows) {
            var input = recipe.inputFluid[0];
            var pressures =
                    indexed.computeIfAbsent(
                            input.type(),
                            _ ->
                                    new CompressorRecipe[FluidTankHandler.TOP_LANE + 1]);
            pressures[input.pressure()] = recipe;
        }
        return Map.copyOf(indexed);
    }

    public @Nullable CompressorRecipe find(@Nullable Fluid type, int pressure) {
        if (type == null) return null;
        var pressures = index().get(type);
        return pressures == null ? null : pressures[pressure];
    }

    public @Nullable CompressorRecipe get(@Nullable Fluid type, int pressure) {
        return find(type, pressure);
    }
}
