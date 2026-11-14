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
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jspecify.annotations.Nullable;

public class LiquefactionRecipes extends SimpleGenericRecipes<LiquefactionRecipe> {

    public static final LiquefactionRecipes INSTANCE = new LiquefactionRecipes();

    @Override
    protected String registryName() {
        return "liquefaction";
    }

    @Override
    protected RecipeSerializer<LiquefactionRecipe> serializer() {
        return LiquefactionRecipe.SERIALIZER;
    }

    public @Nullable FluidStackNTM getOutput(ItemStack stack) {
        if (stack.isEmpty()) return null;

        List<LiquefactionRecipe> candidates = byInputItem().candidates(stack.getItem());
        for (int i = 0; i < candidates.size(); i++) {
            LiquefactionRecipe recipe = candidates.get(i);
            if (recipe.inputItem[0].matchesItem(stack)) return recipe.melt();
        }

        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food != null) {
            int fill = (int) (food.saturation() * 10);
            if (fill > 0) return new FluidStackNTM(NTMFluids.SALIENT, fill);
        }
        return null;
    }
}
