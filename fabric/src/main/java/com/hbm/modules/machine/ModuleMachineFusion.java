/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.modules.machine;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.modules.machine.ModuleMachineBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ModuleMachineFusion extends ModuleMachineBase {

    public double processSpeed = 1D;
    public double bonusSpeed;
    public double bonus;

    public ModuleMachineFusion(
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

    public void preUpdate(double processSpeed, double bonusSpeed) {
        this.processSpeed = processSpeed;
        this.bonusSpeed = bonusSpeed;
    }

    @Override
    protected boolean hasInputFluids(GenericRecipe recipe) {
        if (processSpeed <= 0) return false;
        for (int i = 0; i < Math.min(recipe.inputFluid.length, inputTanks.length); i++) {
            int need = (int) Math.ceil(recipe.inputFluid[i].amount() * processSpeed);
            if (inputTanks[i].getFill() > 0 && inputTanks[i].getFill() < need) return false;
        }
        return true;
    }

    @Override
    public void process(Match match, double speed, double power) {
        GenericRecipe recipe = match.recipe;
        battery()
                .setPower(
                        battery().getPower()
                                - (long) Math.ceil(consumption(recipe, power) * processSpeed));

        double step = Math.min(speed / recipe.duration * processSpeed, 1D);
        advanceProgress(step);
        hbm$syncSet$ModuleMachineFusion$bonus(Math.min(bonus + step * bonusSpeed, 1.5D));

        for (int i = 0; i < Math.min(recipe.inputFluid.length, inputTanks.length); i++) {
            int burnt = (int) Math.ceil(recipe.inputFluid[i].amount() * processSpeed);
            inputTanks[i].setFill(Math.max(inputTanks[i].getFill() - burnt, 0));
        }

        if (progress >= 1D) {
            produceItem(recipe);
            finishProgress(canProcess(recipe, power) != null);
        }

        if (bonus >= 1D && canFitOutput(recipe)) {
            produceItem(recipe);
            hbm$syncSet$ModuleMachineFusion$bonus((double) (bonus - (1D)));
        }
    }

    @Override
    public void serialize(ByteBuf buf) {
        super.serialize(buf);
        buf.writeDouble(bonus);
    }

    @Override
    public void deserialize(ByteBuf buf) {
        super.deserialize(buf);
        hbm$syncSet$ModuleMachineFusion$bonus(buf.readDouble());
    }

    @Override
    public void save(ValueOutput out) {
        super.save(out);
        out.putDouble("bonus" + index, bonus);
    }

    @Override
    public void load(ValueInput in) {
        super.load(in);
        hbm$syncSet$ModuleMachineFusion$bonus(in.getDoubleOr("bonus" + index, bonus));
    }

    public final void hbm$syncSet$ModuleMachineFusion$bonus(double next) {
        if (Double.doubleToRawLongBits(this.bonus) != Double.doubleToRawLongBits(next)) {
            this.bonus = next;
            if ((0L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 0L);
        }
    }
}
