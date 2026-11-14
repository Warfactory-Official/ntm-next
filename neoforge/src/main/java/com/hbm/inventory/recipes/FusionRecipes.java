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

import com.hbm.inventory.recipes.FusionRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import java.util.List;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class FusionRecipes extends GenericRecipes<FusionRecipe, FusionRecipes.Index> {

    public static final FusionRecipes INSTANCE = new FusionRecipes();

    @Override
    protected String registryName() {
        return "fusion";
    }

    @Override
    protected RecipeSerializer<FusionRecipe> serializer() {
        return FusionRecipe.SERIALIZER;
    }

    @Override
    protected Index indexRows(List<FusionRecipe> rows) {
        long highest = 0;
        for (FusionRecipe recipe : rows) {
            if (recipe.ignitionTemp > highest) highest = recipe.ignitionTemp;
        }
        return new Index(highest);
    }

    public long maxInput() {
        return index().maxInput();
    }

    record Index(long maxInput) {}
}
