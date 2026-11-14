/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.inventory.recipes.ingredient.IngredientGroup;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.Container;
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
            if (IngredientGroup.matches(recipe.toppings(), inv, toppingSlots)
                    && IngredientGroup.matches(recipe.pcb(), inv, pcbSlots)
                    && IngredientGroup.matches(recipe.solder(), inv.getItem(soldSlot)))
                return recipe;
        }
        return null;
    }

    record Index(
            List<CountIngredient> toppings,
            List<CountIngredient> pcb,
            List<CountIngredient> solder) {}
}
