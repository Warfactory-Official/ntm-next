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

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class VacuumRefineryRecipes extends SimpleGenericRecipes<VacuumRefineryRecipe> {

    public static final VacuumRefineryRecipes INSTANCE = new VacuumRefineryRecipes();

    public static final int FILL_PER_OP = 100;

    public static final int VAC_FRAC_HEAVY = 40;
    public static final int VAC_FRAC_REFORM = 25;
    public static final int VAC_FRAC_LIGHT = 20;
    public static final int VAC_FRAC_SOUR = 15;

    @Override
    protected String registryName() {
        return "vacuum_refinery";
    }

    @Override
    protected RecipeSerializer<VacuumRefineryRecipe> serializer() {
        return VacuumRefineryRecipe.SERIALIZER;
    }

    public @Nullable VacuumRefineryRecipe getVacuum(@Nullable Fluid oil) {
        return byInputFluid().findLast(oil);
    }
}
