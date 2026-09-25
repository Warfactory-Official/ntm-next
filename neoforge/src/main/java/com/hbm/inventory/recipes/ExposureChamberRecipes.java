// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jspecify.annotations.Nullable;

public class ExposureChamberRecipes extends SimpleGenericRecipes<ExposureChamberRecipe> {

    public static final ExposureChamberRecipes INSTANCE = new ExposureChamberRecipes();

    public static @Nullable ExposureChamberRecipe getRecipe(
            ItemStack particle, ItemStack ingredient) {
        if (particle.isEmpty() || ingredient.isEmpty()) return null;
        return INSTANCE.findByItem(
                particle,
                recipe ->
                        recipe.particle().matchesItem(particle)
                                && recipe.ingredient().matchesItem(ingredient));
    }

    public static boolean anyRowNames(ItemStack stack, boolean asParticle) {
        for (ExposureChamberRecipe recipe : INSTANCE.recipes()) {
            if ((asParticle ? recipe.particle() : recipe.ingredient()).matchesItem(stack))
                return true;
        }
        return false;
    }

    @Override
    protected String registryName() {
        return "exposure_chamber";
    }

    @Override
    protected RecipeSerializer<ExposureChamberRecipe> serializer() {
        return ExposureChamberRecipe.SERIALIZER;
    }
}
