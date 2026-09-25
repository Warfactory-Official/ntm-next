// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class ParticleAcceleratorRecipes extends SimpleGenericRecipes<ParticleAcceleratorRecipe> {

    public static final ParticleAcceleratorRecipes INSTANCE = new ParticleAcceleratorRecipes();

    @Override
    protected String registryName() {
        return "particle_accelerator";
    }

    @Override
    protected RecipeSerializer<ParticleAcceleratorRecipe> serializer() {
        return ParticleAcceleratorRecipe.SERIALIZER;
    }
}
