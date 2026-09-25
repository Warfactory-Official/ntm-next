// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import net.minecraft.resources.Identifier;

public abstract class TablePage<T extends GenericRecipe> extends RecipePage<T> {

    protected TablePage(Identifier id, Class<? extends T> rowType) {
        super(id, rowType);
    }

    @Override
    public Identifier rowId(T row) {
        return row.recipeId().identifier();
    }
}
