// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.crafting;

import com.hbm.inventory.recipes.ingredient.RecipeInputState;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.level.Level;

public abstract class HbmCustomRecipe extends CustomRecipe implements CraftingInputState {
    protected abstract boolean matchesInput(CraftingInput input, Level level);

    @Override
    public final boolean matches(CraftingInput input, Level level) {
        return matchesInput(input, level) && RecipeInputState.crafting(this, input, null, true);
    }
}
