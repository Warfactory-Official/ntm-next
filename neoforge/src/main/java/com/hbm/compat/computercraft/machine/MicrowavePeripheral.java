// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.machine;

import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.BlockEntityMicrowave;
import dan200.computercraft.api.lua.LuaFunction;

public final class MicrowavePeripheral extends SnapshotPeripheral<BlockEntityMicrowave> {

    private static final int SPEED = 0;

    public MicrowavePeripheral(BlockEntityMicrowave machine) {
        super(machine, "microwave", 1, 0);
    }

    @Override
    protected void capture(BlockEntityMicrowave machine) {
        put(SPEED, machine.speed);
    }

    @LuaFunction
    public final Object[] test() {
        return new Object[] {"This is a testing device for everything OC."};
    }

    @LuaFunction
    public final Object[] variableget() {
        return read(s -> new Object[] {s.intAt(SPEED), "test of the `getter` callback function"});
    }

    @LuaFunction(mainThread = true)
    public final Object[] variableset(int speed) {
        BlockEntityMicrowave machine = machine();
        machine.speed = Math.clamp(speed, 0, 5);
        machine.setChanged();
        return new Object[] {"test of the `setter` callback function"};
    }
}
