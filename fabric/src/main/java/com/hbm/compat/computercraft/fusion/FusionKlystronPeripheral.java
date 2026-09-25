// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.fusion;

import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionKlystron;
import dan200.computercraft.api.lua.LuaFunction;

public final class FusionKlystronPeripheral extends SnapshotPeripheral<BlockEntityFusionKlystron> {

    private static final int POWER = 0;
    private static final int MAX_POWER = 1;
    private static final int AIR = 2;
    private static final int AIR_MAX = 3;
    private static final int OUTPUT = 4;
    private static final int TARGET = 5;

    public FusionKlystronPeripheral(BlockEntityFusionKlystron machine) {
        super(machine, "ntm_fusion_klystron", 6, 0);
    }

    @Override
    protected void capture(BlockEntityFusionKlystron machine) {
        put(POWER, machine.getPower());
        put(MAX_POWER, machine.getMaxPower());
        put(AIR, machine.compair.getFill());
        put(AIR_MAX, machine.compair.getMaxFill());
        put(OUTPUT, machine.output);
        put(TARGET, machine.outputTarget);
    }

    @LuaFunction
    public final Object[] getEnergyInfo() {
        return read(s -> new Object[] {s.longAt(POWER), s.longAt(MAX_POWER)});
    }

    @LuaFunction
    public final Object[] getAir() {
        return read(s -> new Object[] {s.intAt(AIR), s.intAt(AIR_MAX)});
    }

    @LuaFunction
    public final Object[] getOutput() {
        return read(s -> new Object[] {s.longAt(OUTPUT), s.longAt(TARGET)});
    }

    @LuaFunction(mainThread = true)
    public final Object[] setOutput(double output) {
        BlockEntityFusionKlystron machine = machine();
        machine.outputTarget = (long) Math.clamp(output, 0.0, BlockEntityFusionKlystron.MAX_OUTPUT);
        machine.setChanged();
        return new Object[] {};
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(
                s ->
                        new Object[] {
                            s.longAt(POWER),
                            s.longAt(MAX_POWER),
                            s.intAt(AIR),
                            s.intAt(AIR_MAX),
                            s.longAt(OUTPUT),
                            s.longAt(TARGET)
                        });
    }
}
