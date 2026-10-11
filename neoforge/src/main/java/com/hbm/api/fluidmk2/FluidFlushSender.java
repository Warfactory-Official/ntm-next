// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.fluidmk2;

import com.hbm.inventory.fluid.tank.FluidTankNTM;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.Fluid;

public interface FluidFlushSender extends IFluidHandlerMK2 {

    FluidTankNTM[] getSendingTanks();

    BlockPos getBlockPos();

    @Override
    default boolean canProvideFluid(Fluid type) {
        for (FluidTankNTM tank : getSendingTanks()) {
            if (tank.joins(type)) return true;
        }
        return false;
    }

    @Override
    default void forEachTank(Consumer<FluidTankNTM> visitor) {
        for (FluidTankNTM tank : getSendingTanks()) visitor.accept(tank);
    }

    void declareFlush(FlushLanes out);
}
