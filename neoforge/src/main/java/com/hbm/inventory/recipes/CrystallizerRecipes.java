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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class CrystallizerRecipes extends SimpleGenericRecipes<CrystallizerRecipe> {

    public static final CrystallizerRecipes INSTANCE = new CrystallizerRecipes();

    @Override
    protected String registryName() {
        return "crystallizer";
    }

    @Override
    protected RecipeSerializer<CrystallizerRecipe> serializer() {
        return CrystallizerRecipe.SERIALIZER;
    }

    public int getAmount(ItemStack stack) {
        CrystallizerRecipe recipe = findByItem(stack, r -> r.inputItem[0].matchesItem(stack));
        return recipe == null ? 0 : recipe.itemAmount();
    }

    public @Nullable CrystallizerRecipe getOutput(ItemStack stack, @Nullable Fluid type) {
        if (stack.isEmpty() || type == null) return null;
        return findByItem(stack, r -> r.acidType() == type && r.inputItem[0].matchesItem(stack));
    }
}
