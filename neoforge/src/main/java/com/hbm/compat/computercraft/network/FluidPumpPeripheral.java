// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.network;

import com.hbm.api.energymk2.IEnergyHandlerMK2.ConnectionPriority;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.tileentity.network.BlockEntityFluidPump;
import dan200.computercraft.api.lua.LuaFunction;
import java.util.Locale;

public final class FluidPumpPeripheral extends SnapshotPeripheral<BlockEntityFluidPump> {

    private static final int PRESSURE = 0;
    private static final int FLOW = 1;
    private static final int FLUID = 0;
    private static final int PRIORITY = 1;

    public FluidPumpPeripheral(BlockEntityFluidPump machine) {
        super(machine, "ntm_fluid_pump", 2, 2);
    }

    @Override
    protected void capture(BlockEntityFluidPump machine) {
        put(PRESSURE, machine.pressure);
        put(FLOW, machine.rate);

        putRef(FLUID, "hbmfluid." + NTMFluids.legacyName(machine.type).toLowerCase(Locale.US));

        putRef(PRIORITY, machine.getFluidPriority().toString());
    }

    @LuaFunction
    public final Object[] getFluid() {
        return read(s -> new Object[] {s.refAt(FLUID)});
    }

    @LuaFunction
    public final Object[] getPressure() {
        return read(s -> new Object[] {s.intAt(PRESSURE)});
    }

    @LuaFunction
    public final Object[] getFlow() {
        return read(s -> new Object[] {s.intAt(FLOW)});
    }

    @LuaFunction
    public final Object[] getPriority() {
        return read(s -> new Object[] {s.refAt(PRIORITY)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(
                s ->
                        new Object[] {
                            s.refAt(FLUID), s.intAt(PRESSURE), s.intAt(FLOW), s.refAt(PRIORITY)
                        });
    }

    @LuaFunction(mainThread = true)
    public final Object[] setPriority(int priority) {
        ConnectionPriority chosen =
                switch (priority) {
                    case 0 -> ConnectionPriority.LOWEST;
                    case 1 -> ConnectionPriority.LOW;
                    case 2 -> ConnectionPriority.NORMAL;
                    case 3 -> ConnectionPriority.HIGH;
                    case 4 -> ConnectionPriority.HIGHEST;
                    default -> null;
                };
        if (chosen == null) return new Object[] {null, "Not a valid Priority."};
        BlockEntityFluidPump machine = machine();
        machine.priority = chosen;
        machine.setChanged();
        return new Object[] {true};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setFlow(int flow) {
        if (flow > 10000 || flow < 0) return new Object[] {null, "Number outside of bounds."};
        BlockEntityFluidPump machine = machine();
        machine.rate = flow;
        machine.setChanged();
        return new Object[] {true};
    }
}
