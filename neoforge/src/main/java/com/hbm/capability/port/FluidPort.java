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

package com.hbm.capability.port;

import com.hbm.api.fluidmk2.ComponentFluidProvider;
import com.hbm.api.fluidmk2.ComponentFluidReceiver;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.capability.NtmCapabilities;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.util.BitMasks;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public final class FluidPort
        implements PortView, ComponentFluidProvider, ComponentFluidReceiver, FluidTankAccess {

    private static final FluidTankNTM[] NO_TANKS = new FluidTankNTM[0];
    private static final FluidPort NONE = new FluidPort(NO_TANKS, 0, 0, null, null);
    private final FluidTankNTM[] tanks;
    private final int receive;
    private final int provide;
    private final @Nullable Filter filter;
    private final @Nullable Runnable onChanged;
    private final int exposed;

    private FluidPort(
            FluidTankNTM[] tanks,
            int receive,
            int provide,
            @Nullable Filter filter,
            @Nullable Runnable onChanged) {
        if (((receive | provide) & ~BitMasks.range32(0, tanks.length)) != 0) {
            throw new IllegalArgumentException("fluid port names a missing tank");
        }
        this.tanks = tanks;
        this.receive = receive;
        this.provide = provide;
        this.filter = filter;
        this.onChanged = onChanged;
        this.exposed = receive | provide;
    }

    @Override
    public int tankCount() {
        return Integer.bitCount(exposed);
    }

    @Override
    public FluidTankNTM tank(int index) {
        return tanks[BitMasks.index(exposed, index)];
    }

    public static FluidPort none() {
        return NONE;
    }

    public static FluidPort of(FluidTankNTM[] tanks, int receive, int provide) {
        return new FluidPort(tanks, receive, provide, null, null);
    }

    public static FluidPort of(FluidTankNTM[] tanks, int receive, int provide, Runnable onChanged) {
        return new FluidPort(tanks, receive, provide, null, onChanged);
    }

    public static FluidPort filtered(
            FluidTankNTM[] tanks, int receive, int provide, Filter filter) {
        return new FluidPort(tanks, receive, provide, filter, null);
    }

    @Override
    public <T> @Nullable T as(Class<T> type, NtmCapabilities.CapRole role) {
        if (role == NtmCapabilities.CapRole.FLUID_IN && receive != 0) return type.cast(this);
        if (role == NtmCapabilities.CapRole.FLUID_OUT && provide != 0) return type.cast(this);
        return null;
    }

    private static boolean takes(
            FluidTankNTM tank, Fluid type, DataComponentPatch components, int pressure) {
        return tank.getPressure() == pressure && tank.accepts(type, components);
    }

    private static boolean gives(
            FluidTankNTM tank, Fluid type, DataComponentPatch components, int pressure) {
        return tank.getPressure() == pressure && tank.provides(type, components);
    }

    @Override
    public boolean canReceiveFluid(Fluid type) {
        if (!filterAllowsType(type)) return false;
        for (var remaining = receive; remaining != 0; remaining &= remaining - 1) {
            var slot = Integer.numberOfTrailingZeros(remaining);
            if (tanks[slot].joins(type)) return true;
        }
        return false;
    }

    @Override
    public boolean canProvideFluid(Fluid type) {
        if (!filterAllowsType(type)) return false;
        for (var remaining = provide; remaining != 0; remaining &= remaining - 1) {
            var slot = Integer.numberOfTrailingZeros(remaining);
            if (tanks[slot].joins(type)) return true;
        }
        return false;
    }

    private boolean filterAllowsType(Fluid type) {
        if (filter == null) return true;
        for (int pressure = 0; pressure <= FluidTankHandler.TOP_LANE; pressure++) {
            if (filter.test(type, pressure)) return true;
        }
        return false;
    }

    @Override
    public long getDemand(Fluid type, DataComponentPatch components, int pressure) {
        if (filter != null && !filter.test(type, pressure)) return 0;
        long demand = 0;
        for (var remaining = receive; remaining != 0; remaining &= remaining - 1) {
            var slot = Integer.numberOfTrailingZeros(remaining);
            FluidTankNTM tank = tanks[slot];
            if (takes(tank, type, components, pressure))
                demand += tank.getMaxFill() - tank.getFill();
        }
        return demand;
    }

    @Override
    public long transferFluid(
            Fluid type, DataComponentPatch components, int pressure, long amount) {
        if (filter != null && !filter.test(type, pressure)) return amount;
        long before = amount;
        for (var remaining = receive; remaining != 0 && amount > 0; remaining &= remaining - 1) {
            var slot = Integer.numberOfTrailingZeros(remaining);
            FluidTankNTM tank = tanks[slot];
            if (!takes(tank, type, components, pressure)) continue;
            amount -= tank.receive(type, components, (int) Math.min(amount, Integer.MAX_VALUE));
        }
        if (amount != before) changed();
        return amount;
    }

    @Override
    public int receivingPressureMask(Fluid type, DataComponentPatch components) {
        return pressureMask(receive, type, components, true);
    }

    @Override
    public long getFluidAvailable(Fluid type, DataComponentPatch components, int pressure) {
        if (filter != null && !filter.test(type, pressure)) return 0;
        long available = 0;
        for (var remaining = provide; remaining != 0; remaining &= remaining - 1) {
            var slot = Integer.numberOfTrailingZeros(remaining);
            FluidTankNTM tank = tanks[slot];
            if (gives(tank, type, components, pressure)) available += tank.getFill();
        }
        return available;
    }

    @Override
    public void useUpFluid(Fluid type, DataComponentPatch components, int pressure, long amount) {
        if (filter != null && !filter.test(type, pressure)) return;
        long before = amount;
        for (var remaining = provide; remaining != 0 && amount > 0; remaining &= remaining - 1) {
            var slot = Integer.numberOfTrailingZeros(remaining);
            FluidTankNTM tank = tanks[slot];
            if (!gives(tank, type, components, pressure)) continue;
            amount -= tank.drain((int) Math.min(amount, Integer.MAX_VALUE), true);
        }
        if (amount != before) changed();
    }

    @Override
    public int providingPressureMask(Fluid type, DataComponentPatch components) {
        return pressureMask(provide, type, components, false);
    }

    @Override
    public void providedComponents(Fluid type, Consumer<DataComponentPatch> visitor) {
        for (var remaining = provide; remaining != 0; remaining &= remaining - 1) {
            var slot = Integer.numberOfTrailingZeros(remaining);
            FluidTankNTM tank = tanks[slot];
            if (tank.getFill() > 0 && tank.getFluid() == type && !tank.getComponents().isEmpty())
                visitor.accept(tank.getComponents());
        }
    }

    private int pressureMask(
            int slots, Fluid type, DataComponentPatch components, boolean receiving) {
        int mask = 0;
        for (var remaining = slots; remaining != 0; remaining &= remaining - 1) {
            var slot = Integer.numberOfTrailingZeros(remaining);
            FluidTankNTM tank = tanks[slot];
            if (receiving ? !tank.accepts(type, components) : !tank.provides(type, components))
                continue;
            mask |= 1 << tank.getPressure();
        }
        return mask;
    }

    private void changed() {
        if (onChanged != null) onChanged.run();
    }

    @FunctionalInterface
    public interface Filter {
        boolean test(Fluid type, int pressure);
    }
}
