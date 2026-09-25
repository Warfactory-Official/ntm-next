// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.modules.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import net.minecraft.world.Container;
import org.jspecify.annotations.Nullable;

public class ModuleMachinePlasma extends ModuleMachineBase {

    private static final int FLOOR = 16_000;

    public ModuleMachinePlasma(
            int index,
            IEnergyHandlerMK2 battery,
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
