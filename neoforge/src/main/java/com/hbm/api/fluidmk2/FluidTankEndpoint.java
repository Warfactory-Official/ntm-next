// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.fluidmk2;

import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.util.ChunkUtil;
import java.util.function.Consumer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public interface FluidTankEndpoint extends FluidFlushSender {

    int HIGHEST_VALID_PRESSURE = 5;

    @Override
    default void declareFlush(FlushLanes out) {}

    @Nullable Level getLevel();

    default FluidTankNTM[] getReceivingTanks() {
        return NO_TANKS;
    }

    @Override
    default FluidTankNTM[] getSendingTanks() {
        return NO_TANKS;
    }

    @Override
    default boolean canReceiveFluid(Fluid type) {
        for (FluidTankNTM tank : getReceivingTanks()) {
            if (tank.joins(type)) return true;
        }
        return false;
    }

    @Override
    default void forEachTank(Consumer<FluidTankNTM> visitor) {
        for (FluidTankNTM tank : getReceivingTanks()) visitor.accept(tank);
        for (FluidTankNTM tank : getSendingTanks()) visitor.accept(tank);
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
    default int receivingPressureMask(Fluid type) {
        return pressureMask(getReceivingTanks(), type, true);
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
    default int providingPressureMask(Fluid type) {
        return pressureMask(getSendingTanks(), type, false);
    }

    private void fluidChanged() {
        if (this instanceof BlockEntity be) ChunkUtil.markUnsaved(be);
    }

    private static boolean takes(FluidTankNTM tank, Fluid type, int pressure) {
        return tank.getPressure() == pressure && tank.accepts(type);
    }

    private static boolean gives(FluidTankNTM tank, Fluid type, int pressure) {
        return tank.getPressure() == pressure && tank.provides(type);
    }

    private static int pressureMask(FluidTankNTM[] tanks, Fluid type, boolean receiving) {
        int mask = 0;
        for (FluidTankNTM tank : tanks) {
            if (receiving ? !tank.accepts(type) : !tank.provides(type)) continue;
            mask |= 1 << tank.getPressure();
        }
        return mask;
    }
}
