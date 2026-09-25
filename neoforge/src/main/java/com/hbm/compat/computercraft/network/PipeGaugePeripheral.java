// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.network;

import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.network.BlockEntityPipeGauge;
import dan200.computercraft.api.lua.LuaFunction;

public final class PipeGaugePeripheral extends SnapshotPeripheral<BlockEntityPipeGauge> {

    private static final int DELTA_TICK = 0;
    private static final int DELTA_SECOND = 1;
    private static final int X = 2;
    private static final int Y = 3;
    private static final int Z = 4;
    private static final int FLUID = 0;

    public PipeGaugePeripheral(BlockEntityPipeGauge machine) {
        super(machine, "ntm_fluid_gauge", 5, 1);
    }

    @Override
    protected void capture(BlockEntityPipeGauge machine) {
        put(DELTA_TICK, machine.deltaTick());
        put(DELTA_SECOND, machine.deltaLastSecond());
        put(X, machine.getBlockPos().getX());
        put(Y, machine.getBlockPos().getY());
        put(Z, machine.getBlockPos().getZ());
        putRef(FLUID, PipeFluid.legacyName(machine));
    }

    @LuaFunction
    public final Object[] getTransfer() {
        return read(s -> new Object[] {s.longAt(DELTA_TICK), s.longAt(DELTA_SECOND)});
    }

    @LuaFunction
    public final Object[] getFluid() {
        return read(s -> new Object[] {s.refAt(FLUID)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(
                s ->
                        new Object[] {
                            s.longAt(DELTA_TICK),
                            s.longAt(DELTA_SECOND),
                            s.refAt(FLUID),
                            s.intAt(X),
                            s.intAt(Y),
                            s.intAt(Z)
                        });
    }
}
