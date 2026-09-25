// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import java.util.Collection;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class SolidificationRecipes extends SimpleGenericRecipes<SolidificationRecipe> {

    public static final SolidificationRecipes INSTANCE = new SolidificationRecipes();

    public static final int SF_OIL = 200, SF_CRACK = 200, SF_HEAVY = 150, SF_BITUMEN = 100;
    public static final int SF_COALOIL = 200, SF_CREOSOTE = 200, SF_WOOD = 1000, SF_LUBE = 100;

    @Override
    protected String registryName() {
        return "solidification";
    }

    @Override
    protected RecipeSerializer<SolidificationRecipe> serializer() {
        return SolidificationRecipe.SERIALIZER;
    }

    public @Nullable SolidificationRecipe getOutput(@Nullable Fluid type) {
        return byInputFluid().findLast(type);
    }

    public Collection<SolidificationRecipe> reachable() {
        return byInputFluid().reachable();
    }
}
