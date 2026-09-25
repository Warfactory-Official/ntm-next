// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.inventory.recipes.loader.NtmRecipeInput;
import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jspecify.annotations.Nullable;

public class AssemblyMachineRecipes extends SimpleGenericRecipes<GenericRecipe> {

    public static final AssemblyMachineRecipes INSTANCE = new AssemblyMachineRecipes();

    public static final String POOL_PLATES = GenericRecipes.POOL_PREFIX_ALT + "plates";

    @Override
    protected String registryName() {
        return "assembly_machine";
    }

    @Override
    protected RecipeSerializer<AssemblyRecipe> serializer() {
        return AssemblyRecipe.SERIALIZER;
    }

    public @Nullable AssemblyRecipe getMatch(ItemStack input) {
        if (input.isEmpty()) return null;
        NtmRecipeInput in = new NtmRecipeInput(1).set(0, input);
        return (AssemblyRecipe)
                findByItem(
                        input,
                        r -> r.inputItem != null && r.inputItem.length == 1 && r.matches(in, null));
    }
}
