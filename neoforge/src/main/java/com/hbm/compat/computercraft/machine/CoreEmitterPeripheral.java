// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.machine;

import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.BlockEntityCoreEmitter;
import dan200.computercraft.api.lua.LuaFunction;

public final class CoreEmitterPeripheral extends SnapshotPeripheral<BlockEntityCoreEmitter> {

    private static final int POWER = 0;
    private static final int MAX_POWER = 1;
    private static final int CRYOGEL = 2;
    private static final int WATTS = 3;
    private static final int ON = 4;

    public CoreEmitterPeripheral(BlockEntityCoreEmitter machine) {
        super(machine, "dfc_emitter", 5, 0);
    }

    @Override
    protected void capture(BlockEntityCoreEmitter machine) {
        put(POWER, machine.getPower());
        put(MAX_POWER, machine.getMaxPower());
        put(CRYOGEL, machine.tank.getFill());
        put(WATTS, machine.watts);
        put(ON, machine.isOn);
    }

    @LuaFunction
    public final Object[] getEnergyInfo() {
        return read(s -> new Object[] {s.longAt(POWER), s.longAt(MAX_POWER)});
    }

    @LuaFunction
    public final Object[] getCryogel() {
        return read(s -> new Object[] {s.intAt(CRYOGEL)});
    }

    @LuaFunction
    public final Object[] getInput() {
        return read(s -> new Object[] {s.intAt(WATTS)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(
                s ->
                        new Object[] {
                            s.longAt(POWER),
                            s.longAt(MAX_POWER),
                            s.intAt(CRYOGEL),
                            s.intAt(WATTS),
                            s.booleanAt(ON)
                        });
    }

    @LuaFunction
    public final Object[] isActive() {
        return read(s -> new Object[] {s.booleanAt(ON)});
    }

    @LuaFunction(mainThread = true)
    public final Object[] setActive(boolean active) {
        BlockEntityCoreEmitter machine = machine();
        machine.isOn = active;
        machine.markChanged();
        return new Object[] {};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setInput(int input) {
        BlockEntityCoreEmitter machine = machine();
        machine.watts = Math.clamp(input, 0, 100);
        machine.markChanged();
        return new Object[] {};
    }
}
