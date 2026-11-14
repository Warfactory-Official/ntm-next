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

public class RefineryRecipes extends SimpleGenericRecipes<RefineryRecipe> {

    public static final RefineryRecipes INSTANCE = new RefineryRecipes();

    public static final int FILL_PER_OP = 100;

    public static final int OIL_FRAC_HEAVY = 50;
    public static final int OIL_FRAC_NAPH = 25;
    public static final int OIL_FRAC_LIGHT = 15;
    public static final int OIL_FRAC_PETRO = 10;
    public static final int CRACK_FRAC_NAPH = 40;
    public static final int CRACK_FRAC_LIGHT = 30;
    public static final int CRACK_FRAC_AROMA = 15;
    public static final int CRACK_FRAC_UNSAT = 15;

    public static final int OILDS_FRAC_HEAVY = 30;
    public static final int OILDS_FRAC_NAPH = 35;
    public static final int OILDS_FRAC_LIGHT = 20;
    public static final int OILDS_FRAC_UNSAT = 15;
    public static final int CRACKDS_FRAC_NAPH = 35;
    public static final int CRACKDS_FRAC_LIGHT = 35;
    public static final int CRACKDS_FRAC_AROMA = 15;
    public static final int CRACKDS_FRAC_UNSAT = 15;

    @Override
    protected String registryName() {
        return "refinery";
    }

    @Override
    protected RecipeSerializer<RefineryRecipe> serializer() {
        return RefineryRecipe.SERIALIZER;
    }

    public @Nullable RefineryRecipe getRefinery(@Nullable Fluid oil) {
        return byInputFluid().findLast(oil);
    }
}
