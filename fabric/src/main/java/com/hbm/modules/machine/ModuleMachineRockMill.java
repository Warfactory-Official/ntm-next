// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.modules.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.RockMillRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import net.minecraft.world.Container;
import org.jspecify.annotations.Nullable;

public class ModuleMachineRockMill extends ModuleMachineBase {
    public ModuleMachineRockMill(
            IEnergyHandlerMK2 battery,
            Container inventory,
            int[] inputSlots,
            int[] outputSlots,
            FluidTankNTM[] inputTanks,
            FluidTankNTM[] outputTanks) {
        super(
                0,
                battery,
                RockMillRecipes.INSTANCE,
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
        sizeTanks(inputTanks, recipe.inputFluid, 4_000);
        sizeTanks(outputTanks, recipe.outputFluid, 4_000);
    }
}
