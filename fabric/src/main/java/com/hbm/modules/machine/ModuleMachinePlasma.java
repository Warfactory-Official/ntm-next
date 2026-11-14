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

package com.hbm.modules.machine;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import net.minecraft.world.Container;
import org.jspecify.annotations.Nullable;

public class ModuleMachinePlasma extends ModuleMachineBase {

    private static final int FLOOR = 16_000;

    public ModuleMachinePlasma(
            int index,
            EnergyHandler battery,
            GenericRecipes<?, ?> recipeSet,
            Container inventory,
            int[] inputSlots,
            int[] outputSlots,
            FluidTankNTM[] inputTanks,
            FluidTankNTM[] outputTanks) {
        super(
                index,
                battery,
                recipeSet,
                inventory,
                inputSlots,
                outputSlots,
                inputTanks,
                outputTanks);
    }

    @Override
    public void setupTanks(@Nullable GenericRecipe recipe) {
        super.setupTanks(recipe);
        if (recipe == null) return;
        sizeTanks(inputTanks, recipe.inputFluid, FLOOR);
    }
}
