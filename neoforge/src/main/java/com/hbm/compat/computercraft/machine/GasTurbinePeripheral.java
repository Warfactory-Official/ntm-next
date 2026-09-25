// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.machine;

import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.tileentity.machine.BlockEntityMachineTurbineGas;
import dan200.computercraft.api.lua.LuaFunction;

public final class GasTurbinePeripheral extends SnapshotPeripheral<BlockEntityMachineTurbineGas> {

    private static final int FILL = 0;
    private static final int POWER = 8;
    private static final int THROTTLE = 9;
    private static final int STATE = 10;
    private static final int AUTO = 11;
    private static final int TYPE = 0;

    public GasTurbinePeripheral(BlockEntityMachineTurbineGas machine) {
        super(machine, "ntm_gas_turbine", 12, 1);
    }

    @Override
    protected void capture(BlockEntityMachineTurbineGas machine) {
        for (int i = 0; i < 4; i++) {
            put(FILL + 2 * i, machine.tanks[i].getFill());
            put(FILL + 2 * i + 1, machine.tanks[i].getMaxFill());
        }
        put(POWER, machine.power);
        put(THROTTLE, machine.throttle);
        put(STATE, machine.state);
        put(AUTO, machine.autoMode);
        putRef(TYPE, NTMFluids.legacyName(machine.tanks[0].getTankType()));
    }

    @LuaFunction
    public final Object[] getFluid() {
        return read(
                s ->
                        new Object[] {
                            s.intAt(0),
                            s.intAt(1),
                            s.intAt(2),
                            s.intAt(3),
                            s.intAt(4),
                            s.intAt(5),
                            s.intAt(6),
                            s.intAt(7)
                        });
    }

    @LuaFunction("getType")
    public final Object[] fluidType() {
        return read(s -> new Object[] {s.refAt(TYPE)});
    }

    @LuaFunction
    public final Object[] getPower() {
        return read(s -> new Object[] {s.longAt(POWER)});
    }

    @LuaFunction
    public final Object[] getThrottle() {
        return read(s -> new Object[] {s.intAt(THROTTLE)});
    }

    @LuaFunction
    public final Object[] getState() {
        return read(s -> new Object[] {s.intAt(STATE)});
    }

    @LuaFunction
    public final Object[] getAuto() {
        return read(s -> new Object[] {s.booleanAt(AUTO)});
    }

    @LuaFunction(mainThread = true)
    public final Object[] setThrottle(int throttle) {
        double input = throttle * 60D / 100D;
        if (input < 0 || input > 100) return new Object[] {null, "Input out of range."};
        BlockEntityMachineTurbineGas machine = machine();
        machine.powerSliderPos = (int) input;
        machine.setChanged();
        return new Object[] {true};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setAuto(boolean auto) {
        BlockEntityMachineTurbineGas machine = machine();
        machine.autoMode = auto;
        machine.setChanged();
        return new Object[] {};
    }

    @LuaFunction(mainThread = true)
    public final Object[] start() {
        BlockEntityMachineTurbineGas machine = machine();
        if (machine.state == 0) machine.state = -1;
        machine.setChanged();
        return new Object[] {};
    }

    @LuaFunction(mainThread = true)
    public final Object[] stop() {
        BlockEntityMachineTurbineGas machine = machine();
        if (machine.state == 1) machine.state = 0;
        machine.setChanged();
        return new Object[] {};
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(
                s ->
                        new Object[] {
                            s.intAt(THROTTLE),
                            s.intAt(STATE),
                            s.intAt(0),
                            s.intAt(1),
                            s.intAt(2),
                            s.intAt(3),
                            s.intAt(4),
                            s.intAt(5),
                            s.intAt(6),
                            s.intAt(7)
                        });
    }
}
