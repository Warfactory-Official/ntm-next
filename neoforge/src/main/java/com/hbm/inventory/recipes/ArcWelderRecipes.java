// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jspecify.annotations.Nullable;

public class ArcWelderRecipes extends SimpleGenericRecipes<ArcWelderRecipe> {

    public static final ArcWelderRecipes INSTANCE = new ArcWelderRecipes();

    public static @Nullable ArcWelderRecipe getRecipe(ItemStack... inputs) {
        outer:
        for (ArcWelderRecipe recipe : INSTANCE.recipes()) {
            List<CountIngredient> recipeList = new ArrayList<>(List.of(recipe.inputItem));

            for (ItemStack inputStack : inputs) {
                if (inputStack.isEmpty()) continue;

                boolean hasMatch = false;
                Iterator<CountIngredient> iterator = recipeList.iterator();

                while (iterator.hasNext()) {
                    CountIngredient recipeStack = iterator.next();
                    if (recipeStack.test(inputStack)) {
                        hasMatch = true;
                        iterator.remove();
                        break;
                    }
                }

                if (!hasMatch) continue outer;
            }

            if (recipeList.isEmpty()) return recipe;
        }

        return null;
    }

    @Override
    protected String registryName() {
        return "arc_welder";
    }

    @Override
    protected RecipeSerializer<ArcWelderRecipe> serializer() {
        return ArcWelderRecipe.SERIALIZER;
    }
}
