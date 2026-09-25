// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.loader;

import net.minecraft.world.item.crafting.RecipeBookCategory;

public interface INamedRecipe {

    RecipeBookCategory BOOK_CATEGORY = new RecipeBookCategory();

    String getInternalName();
}
