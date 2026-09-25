// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.fluidmk2;

import com.hbm.api.energymk2.IEnergyHandlerMK2.ConnectionPriority;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.uninos.IBufferedEndpoint;
import net.minecraft.world.level.material.Fluid;

public interface IFluidHandlerMK2 extends IBufferedEndpoint {

    FluidTankNTM[] NO_TANKS = new FluidTankNTM[0];
    int[] DEFAULT_PRESSURE_RANGE = {0, 0};

    default long getFluidAvailable(Fluid type, int pressure) {
        return 0L;
    }

    default void useUpFluid(Fluid type, int pressure, long amount) {}

    default int[] getProvidingPressureRange(Fluid type) {
        return DEFAULT_PRESSURE_RANGE;
    }

    default long getProviderSpeed(Fluid type, int pressure) {
        return Long.MAX_VALUE;
    }

    default long getDemand(Fluid type, int pressure) {
        return 0L;
    }

    default long transferFluid(Fluid type, int pressure, long amount) {
        return amount;
    }

    default int[] getReceivingPressureRange(Fluid type) {
        return DEFAULT_PRESSURE_RANGE;
    }

    default long getReceiverSpeed(Fluid type, int pressure) {
        return Long.MAX_VALUE;
    }

    default boolean netSubscribed() {
        return true;
    }

    default ConnectionPriority getFluidPriority() {
        return ConnectionPriority.NORMAL;
    }

    default FluidTankNTM[] getAllTanks() {
        return NO_TANKS;
    }
}
