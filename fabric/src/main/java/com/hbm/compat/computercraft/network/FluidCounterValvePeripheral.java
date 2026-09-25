// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.network;

import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.network.BlockEntityFluidCounterValve;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

public final class FluidCounterValvePeripheral
        extends SnapshotPeripheral<BlockEntityFluidCounterValve> {

    private static final int COUNTER = 0;
    private static final int OPEN = 1;
    private static final int FLUID = 0;

    public FluidCounterValvePeripheral(BlockEntityFluidCounterValve machine) {
        super(machine, "ntm_fluid_counter_valve", 2, 1);
    }

    @Override
    protected void capture(BlockEntityFluidCounterValve machine) {
        put(COUNTER, machine.getCounter());
        put(OPEN, machine.isOpen());
        putRef(FLUID, PipeFluid.legacyName(machine));
    }

    @LuaFunction
    public final Object[] getFluid() {
        return read(s -> new Object[] {s.refAt(FLUID)});
    }

    @LuaFunction
    public final Object[] getCounter() {
        return read(s -> new Object[] {s.longAt(COUNTER)});
    }

    @LuaFunction(mainThread = true)
    public final Object[] resetCounter() {
        machine().resetCounter();
        return new Object[] {};
    }

    @LuaFunction
    public final Object[] getState() {
        return read(s -> new Object[] {s.booleanAt(OPEN) ? 1 : 0});
    }

    @LuaFunction(mainThread = true)
    public final Object[] setState(int state) throws LuaException {

        if (state != 0 && state != 1) throw new LuaException("bad argument");
        machine().setState(state);
        return new Object[] {};
    }
}
