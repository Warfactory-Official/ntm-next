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

public class CyclotronRecipes extends SimpleGenericRecipes<CyclotronRecipe> {

    public static final CyclotronRecipes INSTANCE = new CyclotronRecipes();

    public static @Nullable CyclotronRecipe getOutput(ItemStack target, ItemStack particle) {
        if (target.isEmpty() || particle.isEmpty()) return null;
        return INSTANCE.findByItem(
                particle,
                recipe ->
                        recipe.particle().matchesItem(particle)
                                && recipe.target().matchesItem(target));
    }

    public static boolean isParticle(ItemStack stack) {
        return INSTANCE.findByItem(stack, recipe -> recipe.particle().matchesItem(stack)) != null;
    }

    public static boolean isTarget(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (CyclotronRecipe recipe : INSTANCE.recipes()) {
            if (recipe.target().matchesItem(stack)) return true;
        }
        return false;
    }

    @Override
    protected String registryName() {
        return "cyclotron";
    }

    @Override
    protected RecipeSerializer<CyclotronRecipe> serializer() {
        return CyclotronRecipe.SERIALIZER;
    }
}
