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
import com.hbm.items.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jspecify.annotations.Nullable;

public class ShredderRecipes extends SimpleGenericRecipes<ShredderRecipe> {

    public static final ShredderRecipes INSTANCE = new ShredderRecipes();

    public static ItemStack getShredderResult(ItemStack stack) {
        ShredderRecipe recipe = getRecipe(stack);
        if (recipe != null) return recipe.output().copy();
        return stack.getComponentsPatch().isEmpty()
                ? new ItemStack(ModItems.SCRAP)
                : ItemStack.EMPTY;
    }

    public static ItemStack shred(ItemStack stack) {
        ShredderRecipe recipe = getRecipe(stack);
        return recipe != null ? recipe.roll() : getShredderResult(stack);
    }

    public static @Nullable ShredderRecipe getRecipe(ItemStack stack) {
        if (stack.isEmpty()) return null;
        return INSTANCE.findByItem(stack, recipe -> recipe.input().matchesItem(stack));
    }

    @Override
    protected String registryName() {
        return "shredder";
    }

    @Override
    protected RecipeSerializer<ShredderRecipe> serializer() {
        return ShredderRecipe.SERIALIZER;
    }
}
