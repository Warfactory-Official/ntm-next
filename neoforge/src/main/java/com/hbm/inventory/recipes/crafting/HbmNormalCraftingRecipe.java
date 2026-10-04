// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
