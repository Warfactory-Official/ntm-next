// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.capability.port;

import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.capability.NtmCapabilities;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public final class FluidPort implements PortView, IFluidHandlerMK2 {

    private static final FluidTankNTM[] NO_TANKS = new FluidTankNTM[0];
    private static final int[] NO_SLOTS = new int[0];
    private static final FluidPort NONE = new FluidPort(NO_TANKS, NO_SLOTS, NO_SLOTS, null, null);
    private final FluidTankNTM[] tanks;
    private final int[] receive;
    private final int[] provide;
    private final @Nullable Filter filter;
    private final @Nullable Runnable onChanged;

    private FluidPort(
            FluidTankNTM[] tanks,
            int[] receive,
            int[] provide,
            @Nullable Filter filter,
            @Nullable Runnable onChanged) {
        this.tanks = tanks;
        this.receive = receive;
        this.provide = provide;
        this.filter = filter;
        this.onChanged = onChanged;
    }

    public static FluidPort none() {
        return NONE;
    }

    public static FluidPort of(FluidTankNTM[] tanks, int[] receive, int[] provide) {
        return new FluidPort(tanks, receive, provide, null, null);
    }

    public static FluidPort of(
            FluidTankNTM[] tanks, int[] receive, int[] provide, Runnable onChanged) {
        return new FluidPort(tanks, receive, provide, null, onChanged);
    }

    public static FluidPort filtered(
            FluidTankNTM[] tanks, int[] receive, int[] provide, Filter filter) {
        return new FluidPort(tanks, receive, provide, filter, null);
    }

    @Override
    public <T> @Nullable T as(Class<T> type, NtmCapabilities.CapRole role) {
        if (role == NtmCapabilities.CapRole.FLUID_IN && receive.length > 0) return type.cast(this);
        if (role == NtmCapabilities.CapRole.FLUID_OUT && provide.length > 0) return type.cast(this);
        return null;
    }

    private static boolean takes(FluidTankNTM tank, Fluid type, int pressure) {
        return tank.getPressure() == pressure && tank.accepts(type);
    }

    private static boolean gives(FluidTankNTM tank, Fluid type, int pressure) {
        return tank.getPressure() == pressure && tank.provides(type);
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        if (filter != null && !filter.test(type, pressure)) return 0;
        long demand = 0;
        for (int slot : receive) {
            FluidTankNTM tank = tanks[slot];
            if (takes(tank, type, pressure)) demand += tank.getMaxFill() - tank.getFill();
        }
        return demand;
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (filter != null && !filter.test(type, pressure)) return amount;
        long before = amount;
        for (int slot : receive) {
            if (amount <= 0) break;
            FluidTankNTM tank = tanks[slot];
            if (!takes(tank, type, pressure)) continue;
            amount -= tank.fill(type, (int) Math.min(amount, Integer.MAX_VALUE), true);
        }
        if (amount != before) changed();
        return amount;
    }

    @Override
    public int[] getReceivingPressureRange(Fluid type) {
        return range(receive, type, true);
    }

    @Override
    public long getFluidAvailable(Fluid type, int pressure) {
        if (filter != null && !filter.test(type, pressure)) return 0;
        long available = 0;
        for (int slot : provide) {
            FluidTankNTM tank = tanks[slot];
            if (gives(tank, type, pressure)) available += tank.getFill();
        }
        return available;
    }

    @Override
    public void useUpFluid(Fluid type, int pressure, long amount) {
        if (filter != null && !filter.test(type, pressure)) return;
        long before = amount;
        for (int slot : provide) {
            if (amount <= 0) break;
            FluidTankNTM tank = tanks[slot];
            if (!gives(tank, type, pressure)) continue;
            amount -= tank.drain((int) Math.min(amount, Integer.MAX_VALUE), true);
        }
        if (amount != before) changed();
    }

    @Override
    public int[] getProvidingPressureRange(Fluid type) {
        return range(provide, type, false);
    }

    private int[] range(int[] slots, Fluid type, boolean receiving) {
        int lowest = FluidTankEndpoint.HIGHEST_VALID_PRESSURE;
        int highest = 0;
        for (int slot : slots) {
            FluidTankNTM tank = tanks[slot];
            if (receiving ? !tank.accepts(type) : !tank.provides(type)) continue;
            if (tank.getPressure() < lowest) lowest = tank.getPressure();
            if (tank.getPressure() > highest) highest = tank.getPressure();
        }
        return lowest <= highest ? new int[] {lowest, highest} : DEFAULT_PRESSURE_RANGE;
    }

    private void changed() {
        if (onChanged != null) onChanged.run();
    }

    @FunctionalInterface
    public interface Filter {
        boolean test(Fluid type, int pressure);
    }
}
