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
import org.jspecify.annotations.Nullable;

public class ElectrolyserMetalRecipes extends SimpleGenericRecipes<ElectrolyserMetalRecipe> {

    public static final ElectrolyserMetalRecipes INSTANCE = new ElectrolyserMetalRecipes();

    @Override
    protected String registryName() {
        return "electrolyser_metal";
    }

    @Override
    protected RecipeSerializer<ElectrolyserMetalRecipe> serializer() {
        return ElectrolyserMetalRecipe.SERIALIZER;
    }

    public @Nullable ElectrolyserMetalRecipe getRecipe(ItemStack stack) {
        if (stack.isEmpty()) return null;
        return findByItem(stack, r -> r.inputItem[0].matchesItem(stack));
    }
}
