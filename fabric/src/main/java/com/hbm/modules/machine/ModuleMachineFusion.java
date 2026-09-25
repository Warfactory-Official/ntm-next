// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.modules.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.packet.SyncField;
import io.netty.buffer.ByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class ModuleMachineFusion extends ModuleMachineBase {

    public double processSpeed = 1D;
    public double bonusSpeed;
    @SyncField public double bonus;

    public ModuleMachineFusion(
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
        progress += step;
        bonus = Math.min(bonus + step * bonusSpeed, 1.5D);

        for (int i = 0; i < Math.min(recipe.inputFluid.length, inputTanks.length); i++) {
            int burnt = (int) Math.ceil(recipe.inputFluid[i].amount() * processSpeed);
            inputTanks[i].setFill(Math.max(inputTanks[i].getFill() - burnt, 0));
        }

        if (progress >= 1D) {
            produceItem(recipe);
            progress = canProcess(recipe, power) != null ? progress - 1D : 0D;
        }

        if (bonus >= 1D && canFitOutput(recipe)) {
            produceItem(recipe);
            bonus -= 1D;
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
        bonus = buf.readDouble();
    }

    @Override
    public void save(ValueOutput out) {
        super.save(out);
        out.putDouble("bonus" + index, bonus);
    }

    @Override
    public void load(ValueInput in) {
        super.load(in);
        bonus = in.getDoubleOr("bonus" + index, bonus);
    }
}
