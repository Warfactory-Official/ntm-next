// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.fluidmk2;

import com.hbm.inventory.fluid.tank.FluidTankNTM;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public interface FluidTankEndpoint extends FluidFlushSender {

    int HIGHEST_VALID_PRESSURE = 5;

    @Nullable Level getLevel();

    default FluidTankNTM[] getReceivingTanks() {
        return NO_TANKS;
    }

    @Override
    default FluidTankNTM[] getSendingTanks() {
        return NO_TANKS;
    }

    @Override
    default long getDemand(Fluid type, int pressure) {
        long amount = 0;
        for (FluidTankNTM tank : getReceivingTanks()) {
            if (takes(tank, type, pressure)) amount += (long) tank.getMaxFill() - tank.getFill();
        }
        return amount;
    }

    @Override
    default long transferFluid(Fluid type, int pressure, long amount) {
        FluidTankNTM[] tanks = getReceivingTanks();
        int matching = 0;
        for (FluidTankNTM tank : tanks) {
            if (takes(tank, type, pressure)) matching++;
        }
        if (matching == 0) return amount;
        long refused = amount;

        if (matching > 1) {
            int share = (int) Math.min(refused / matching, Integer.MAX_VALUE);
            for (FluidTankNTM tank : tanks) {
                if (!takes(tank, type, pressure)) continue;
                refused -= tank.receive(type, share);
            }
        }
        if (refused > 0) {
            for (FluidTankNTM tank : tanks) {
                if (!takes(tank, type, pressure)) continue;
                refused -= tank.receive(type, (int) Math.min(refused, Integer.MAX_VALUE));
            }
        }
        if (refused != amount) fluidChanged();
        return refused;
    }

    @Override
    default int[] getReceivingPressureRange(Fluid type) {
        return range(getReceivingTanks(), type, true);
    }

    @Override
    default long getFluidAvailable(Fluid type, int pressure) {
        long amount = 0;
        for (FluidTankNTM tank : getSendingTanks()) {
            if (gives(tank, type, pressure)) amount += tank.getFill();
        }
        return amount;
    }

    @Override
    default void useUpFluid(Fluid type, int pressure, long amount) {
        FluidTankNTM[] tanks = getSendingTanks();
        int matching = 0;
        for (FluidTankNTM tank : tanks) {
            if (gives(tank, type, pressure)) matching++;
        }
        if (matching == 0) return;
        long remaining = amount;

        if (matching > 1) {
            int share = (int) Math.min(remaining / matching, Integer.MAX_VALUE);
            for (FluidTankNTM tank : tanks) {
                if (!gives(tank, type, pressure)) continue;
                int toRemove = Math.min(share, tank.getFill());
                tank.setFill(tank.getFill() - toRemove);
                remaining -= toRemove;
            }
        }
        if (remaining > 0) {
            for (FluidTankNTM tank : tanks) {
                if (!gives(tank, type, pressure)) continue;
                int toRemove = (int) Math.min(remaining, tank.getFill());
                tank.setFill(tank.getFill() - toRemove);
                remaining -= toRemove;
            }
        }
        if (remaining != amount) fluidChanged();
    }

    @Override
    default int[] getProvidingPressureRange(Fluid type) {
        return range(getSendingTanks(), type, false);
    }

    private void fluidChanged() {
        Level level = getLevel();
        if (level != null) level.blockEntityChanged(getBlockPos());
    }

    private static boolean takes(FluidTankNTM tank, Fluid type, int pressure) {
        return tank.getPressure() == pressure && tank.accepts(type);
    }

    private static boolean gives(FluidTankNTM tank, Fluid type, int pressure) {
        return tank.getPressure() == pressure && tank.provides(type);
    }

    private static int[] range(FluidTankNTM[] tanks, Fluid type, boolean receiving) {
        int lowest = HIGHEST_VALID_PRESSURE;
        int highest = 0;
        for (FluidTankNTM tank : tanks) {
            if (receiving ? !tank.accepts(type) : !tank.provides(type)) continue;
            if (tank.getPressure() < lowest) lowest = tank.getPressure();
            if (tank.getPressure() > highest) highest = tank.getPressure();
        }
        return lowest <= highest ? new int[] {lowest, highest} : DEFAULT_PRESSURE_RANGE;
    }
}
