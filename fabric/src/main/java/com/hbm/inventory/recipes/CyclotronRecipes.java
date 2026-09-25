// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jspecify.annotations.Nullable;

public class CyclotronRecipes extends SimpleGenericRecipes<CyclotronRecipe> {

    public static final CyclotronRecipes INSTANCE = new CyclotronRecipes();

    public static @Nullable CyclotronRecipe getOutput(ItemStack target, ItemStack particle) {
        if (target.isEmpty() || particle.isEmpty()) return null;
        return INSTANCE.findByItem(
                particle,
                recipe ->
                        recipe.particle().matchesItem(particle)
                                && recipe.target().matchesItem(target));
    }

    public static boolean isParticle(ItemStack stack) {
        return INSTANCE.findByItem(stack, recipe -> recipe.particle().matchesItem(stack)) != null;
    }

    public static boolean isTarget(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (CyclotronRecipe recipe : INSTANCE.recipes()) {
            if (recipe.target().matchesItem(stack)) return true;
        }
        return false;
    }

    @Override
    protected String registryName() {
        return "cyclotron";
    }

    @Override
    protected RecipeSerializer<CyclotronRecipe> serializer() {
        return CyclotronRecipe.SERIALIZER;
    }
}
