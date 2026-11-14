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

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import com.hbm.items.machine.StampItem.StampType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jspecify.annotations.Nullable;

public class PressRecipes extends SimpleGenericRecipes<PressRecipe> {

    public static final PressRecipes INSTANCE = new PressRecipes();

    @Override
    protected String registryName() {
        return "press";
    }

    @Override
    protected RecipeSerializer<PressRecipe> serializer() {
        return PressRecipe.SERIALIZER;
    }

    public ItemStack getOutput(ItemStack input, @Nullable StampType stampType) {
        PressRecipe recipe = getRecipe(input, stampType);
        return recipe == null ? ItemStack.EMPTY : recipe.output().copy();
    }

    public @Nullable PressRecipe getRecipe(ItemStack input, @Nullable StampType stampType) {
        if (input.isEmpty() || stampType == null) return null;
        return findByItem(
                input, r -> r.stampType == stampType && r.inputItem[0].matchesItem(input));
    }
}
