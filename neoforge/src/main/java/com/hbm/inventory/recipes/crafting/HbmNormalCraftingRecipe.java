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

package com.hbm.inventory.recipes.crafting;

import com.hbm.inventory.recipes.ingredient.RecipeInputState;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.NormalCraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;

public abstract class HbmNormalCraftingRecipe extends NormalCraftingRecipe
        implements CraftingInputState {
    protected HbmNormalCraftingRecipe(
            Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo) {
        super(commonInfo, bookInfo);
    }

    protected abstract boolean matchesInput(CraftingInput input, Level level);

    @Override
    public final boolean matches(CraftingInput input, Level level) {
        return matchesInput(input, level) && RecipeInputState.crafting(this, input, null, true);
    }
}
