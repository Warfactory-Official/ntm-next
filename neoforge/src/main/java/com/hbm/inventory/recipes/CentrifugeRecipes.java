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

import com.hbm.inventory.recipes.loader.NtmRecipeInput;
import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class CentrifugeRecipes extends SimpleGenericRecipes<CentrifugeRecipe> {

    public static final CentrifugeRecipes INSTANCE = new CentrifugeRecipes();

    @Override
    protected String registryName() {
        return "centrifuge";
    }

    @Override
    protected RecipeSerializer<CentrifugeRecipe> serializer() {
        return CentrifugeRecipe.SERIALIZER;
    }

    public @Nullable CentrifugeRecipe matchFor(ItemStack input, Level level) {
        if (input.isEmpty()) return null;
        NtmRecipeInput in = new NtmRecipeInput(1).set(0, input);
        return findByItem(input, r -> r.matches(in, level));
    }

    public ItemStack @Nullable [] getOutputs(ItemStack input, Level level) {
        if (input.isEmpty()) return null;
        NtmRecipeInput in = new NtmRecipeInput(1).set(0, input);
        CentrifugeRecipe r = findByItem(input, row -> row.matches(in, level));
        if (r == null) return null;
        WeightedList<ItemStack>[] outs = r.outputItems();
        ItemStack[] result = new ItemStack[outs.length];
        for (int i = 0; i < outs.length; i++) {
            result[i] = outs[i].getRandom(RNG).orElse(ItemStack.EMPTY).copy();
        }
        return result;
    }
}
