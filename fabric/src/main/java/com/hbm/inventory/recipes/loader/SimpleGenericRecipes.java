// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.loader;

import java.util.List;
import org.jspecify.annotations.Nullable;

public abstract class SimpleGenericRecipes<T extends GenericRecipe>
        extends GenericRecipes<T, Void> {

    @Override
    protected final @Nullable Void indexRows(List<T> rows) {
        return null;
    }
}
