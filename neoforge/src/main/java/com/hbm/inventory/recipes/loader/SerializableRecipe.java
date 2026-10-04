// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.loader;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeType;

public abstract class SerializableRecipe {
    public static final List<SerializableRecipe> recipeHandlers = new ArrayList<>();

    public abstract void registerType();

    public abstract RecipeType<?> datapackType();

    public abstract Identifier datapackTypeId();

    public abstract void ensureFilled();
}
