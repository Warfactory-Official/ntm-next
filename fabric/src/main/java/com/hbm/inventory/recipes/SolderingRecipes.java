// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jspecify.annotations.Nullable;

public class SolderingRecipes extends GenericRecipes<SolderingRecipe, SolderingRecipes.Index> {

    public static final SolderingRecipes INSTANCE = new SolderingRecipes();

    public static List<CountIngredient> toppings() {
        return INSTANCE.index().toppings();
    }

    public static List<CountIngredient> pcb() {
        return INSTANCE.index().pcb();
    }

    public static List<CountIngredient> solder() {
        return INSTANCE.index().solder();
    }

    public static boolean matchesGroup(Container inv, int[] slots, CountIngredient[] group) {
        boolean[] used = new boolean[group.length];
        for (int slot : slots) {
            ItemStack stack = inv.getItem(slot);
            if (stack.isEmpty()) continue;
            boolean matched = false;
            for (int i = 0; i < group.length; i++) {
                if (!used[i] && group[i].test(stack)) {
                    used[i] = true;
                    matched = true;
                    break;
                }
            }
            if (!matched) return false;
        }
        for (boolean u : used) if (!u) return false;
        return true;
    }

    @Override
    protected String registryName() {
        return "soldering";
    }

    @Override
    protected RecipeSerializer<SolderingRecipe> serializer() {
        return SolderingRecipe.SERIALIZER;
    }

    @Override
    protected Index indexRows(List<SolderingRecipe> rows) {
        List<CountIngredient> toppings = new ArrayList<>();
        List<CountIngredient> pcb = new ArrayList<>();
        List<CountIngredient> solder = new ArrayList<>();
        for (SolderingRecipe recipe : rows) {
            toppings.addAll(List.of(recipe.toppings()));
            pcb.addAll(List.of(recipe.pcb()));
            solder.addAll(List.of(recipe.solder()));
        }
        return new Index(List.copyOf(toppings), List.copyOf(pcb), List.copyOf(solder));
    }

    public @Nullable SolderingRecipe getRecipe(
            Container inv, int[] toppingSlots, int[] pcbSlots, int soldSlot) {
        for (SolderingRecipe recipe : recipes()) {
            if (matchesGroup(inv, toppingSlots, recipe.toppings())
                    && matchesGroup(inv, pcbSlots, recipe.pcb())
                    && matchesGroup(inv, new int[] {soldSlot}, recipe.solder())) return recipe;
        }
        return null;
    }

    record Index(
            List<CountIngredient> toppings,
            List<CountIngredient> pcb,
            List<CountIngredient> solder) {}
}
