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

package com.hbm.api.fluidmk2;

import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.util.ChunkUtil;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;

public interface FluidTankHandler
        extends ComponentFluidProvider, ComponentFluidReceiver, FluidFlushSender {
    int TOP_LANE = 5;

    long REQUEST_LIMIT = 1_000_000_000L;

    default FluidTankNTM[] getReceivingTanks() {
        return NO_TANKS;
    }

    @Override
    default FluidTankNTM[] getSendingTanks() {
        return NO_TANKS;
    }

    @Override
    default void declareFlush(FlushLanes out) {}

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
    default void providedComponents(Fluid type, Consumer<DataComponentPatch> visitor) {
        for (FluidTankNTM tank : getSendingTanks()) {
            DataComponentPatch patch = tank.getComponents();
            if (!patch.isEmpty() && tank.getFill() > 0 && tank.getFluid() == type)
                visitor.accept(patch);
        }
    }

    @Override
    default long getDemand(Fluid type, DataComponentPatch components, int pressure) {
        long demand = 0L;
        for (FluidTankNTM tank : getReceivingTanks()) {
            if (tank.getPressure() == pressure && tank.accepts(type, components)) {
                demand += tank.getMaxFill() - tank.getFill();
            }
        }
        return demand;
    }

    @Override
    default long transferFluid(
            Fluid type, DataComponentPatch components, int pressure, long amount) {
        FluidTankNTM[] tanks = getReceivingTanks();
        int takers = 0;
        for (FluidTankNTM tank : tanks) {
            if (tank.getPressure() == pressure && tank.accepts(type, components)) takers++;
        }
        if (takers == 0) return amount;
        long refused = amount;
        if (takers > 1) {
            int share = (int) Math.min(amount / takers, Integer.MAX_VALUE);
            for (FluidTankNTM tank : tanks) {
                if (tank.getPressure() == pressure && tank.accepts(type, components)) {
                    refused -= tank.receive(type, components, share);
                }
            }
        }
        for (int i = 0; i < tanks.length && refused > 0L; i++) {
            FluidTankNTM tank = tanks[i];
            if (tank.getPressure() == pressure && tank.accepts(type, components)) {
                refused -=
                        tank.receive(type, components, (int) Math.min(refused, Integer.MAX_VALUE));
            }
        }
        if (refused != amount) onFluidMoved();
        return refused;
    }

    @Override
    default int receivingPressureMask(Fluid type, DataComponentPatch components) {
        int mask = 0;
        for (FluidTankNTM tank : getReceivingTanks()) {
            if (tank.accepts(type, components)) mask |= 1 << tank.getPressure();
        }
        return mask;
    }

    @Override
    default long getReceiverSpeed(Fluid type, int pressure) {
        return REQUEST_LIMIT;
    }

    @Override
    default long getFluidAvailable(Fluid type, DataComponentPatch components, int pressure) {
        long available = 0L;
        for (FluidTankNTM tank : getSendingTanks()) {
            if (tank.getPressure() == pressure && tank.provides(type, components))
                available += tank.getFill();
        }
        return available;
    }

    @Override
    default void useUpFluid(Fluid type, DataComponentPatch components, int pressure, long amount) {
        FluidTankNTM[] tanks = getSendingTanks();
        int givers = 0;
        for (FluidTankNTM tank : tanks) {
            if (tank.getPressure() == pressure && tank.provides(type, components)) givers++;
        }
        if (givers == 0) return;
        long remaining = amount;
        if (givers > 1) {
            int share = (int) Math.min(amount / givers, Integer.MAX_VALUE);
            for (FluidTankNTM tank : tanks) {
                if (tank.getPressure() == pressure && tank.provides(type, components)) {
                    int taken = Math.min(share, tank.getFill());
                    if (taken > 0) {
                        tank.setFill(tank.getFill() - taken);
                        remaining -= taken;
                    }
                }
            }
        }
        for (int i = 0; i < tanks.length && remaining > 0L; i++) {
            FluidTankNTM tank = tanks[i];
            if (tank.getPressure() == pressure && tank.provides(type, components)) {
                int taken = (int) Math.min(remaining, tank.getFill());
                if (taken > 0) {
                    tank.setFill(tank.getFill() - taken);
                    remaining -= taken;
                }
            }
        }
        if (remaining != amount) onFluidMoved();
    }

    @Override
    default int providingPressureMask(Fluid type, DataComponentPatch components) {
        int mask = 0;
        for (FluidTankNTM tank : getSendingTanks()) {
            if (tank.provides(type, components)) mask |= 1 << tank.getPressure();
        }
        return mask;
    }

    @Override
    default long getProviderSpeed(Fluid type, int pressure) {
        return REQUEST_LIMIT;
    }

    default void onFluidMoved() {
        if (this instanceof BlockEntity be) ChunkUtil.markUnsaved(be);
    }
}
