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

package com.hbm.blockentity.machine.thermal;

import com.hbm.api.fluidmk2.FluidFlushSender;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.fluidmk2.PlainFluidProvider;
import com.hbm.api.fluidmk2.PlainFluidReceiver;
import com.hbm.blockentity.Synced;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import java.util.function.Consumer;
import net.minecraft.world.level.material.Fluid;

public interface ThermalFluidHandler
        extends PlainFluidReceiver, PlainFluidProvider, FluidFlushSender, FluidTankAccess {
    FluidTankNTM receivingTank();

    FluidTankNTM deliveryTank();

    @Override
    default boolean canReceiveFluid(Fluid fluid) {
        return receivingTank().joins(fluid);
    }

    @Override
    default long getDemand(Fluid fluid, int pressure) {
        var tank = receivingTank();
        return tank.getPressure() == pressure && tank.accepts(fluid)
                ? tank.getMaxFill() - tank.getFill()
                : 0;
    }

    @Override
    default long transferFluid(Fluid fluid, int pressure, long amount) {
        var tank = receivingTank();
        if (tank.getPressure() != pressure || !tank.accepts(fluid)) return amount;
        int accepted = tank.receive(fluid, (int) Math.min(amount, Integer.MAX_VALUE));
        if (accepted != 0) ((Synced) this).markChanged();
        return amount - accepted;
    }

    @Override
    default long getFluidAvailable(Fluid fluid, int pressure) {
        var tank = deliveryTank();
        return tank.getPressure() == pressure && tank.provides(fluid) ? tank.getFill() : 0;
    }

    @Override
    default void useUpFluid(Fluid fluid, int pressure, long amount) {
        var tank = deliveryTank();
        if (tank.getPressure() == pressure && tank.provides(fluid) && amount != 0) {
            tank.setFill(tank.getFill() - (int) amount);
            ((Synced) this).markChanged();
        }
    }

    @Override
    default int receivingPressureMask(Fluid fluid) {
        return receivingTank().accepts(fluid) ? 1 << receivingTank().getPressure() : 0;
    }

    @Override
    default int providingPressureMask(Fluid fluid) {
        return deliveryTank().provides(fluid) ? 1 << deliveryTank().getPressure() : 0;
    }

    @Override
    default void forEachTank(Consumer<FluidTankNTM> visitor) {
        visitor.accept(receivingTank());
        visitor.accept(deliveryTank());
    }

    @Override
    default void declareFlush(FlushLanes lanes) {
        lanes.add(deliveryTank(), FlushFaces.own());
    }
}
