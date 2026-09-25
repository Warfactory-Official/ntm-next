// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.rbmk;

import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControl;
import dan200.computercraft.api.lua.LuaFunction;

public class RBMKControlPeripheral<BE extends BlockEntityRBMKControl>
        extends RBMKColumnPeripheral<BE> {

    protected static final int LEVEL = SLOTS;
    protected static final int TARGET = SLOTS + 1;
    protected static final int CONTROL_SLOTS = SLOTS + 2;

    public RBMKControlPeripheral(BE machine) {
        this(machine, CONTROL_SLOTS);
    }

    protected RBMKControlPeripheral(BE machine, int longs) {
        super(machine, "rbmk_control_rod", longs, 0);
    }

    @Override
    protected void capture(BE machine) {
        super.capture(machine);
        put(LEVEL, machine.getMult());
        put(TARGET, machine.targetLevel);
    }

    @LuaFunction
    public final Object[] getLevel() {
        return read(s -> new Object[] {s.doubleAt(LEVEL) * 100});
    }

    @LuaFunction
    public final Object[] getTargetLevel() {
        return read(s -> new Object[] {s.doubleAt(TARGET) * 100});
    }

    @LuaFunction
    public final Object[] getHeat() {
        return read(s -> new Object[] {s.doubleAt(HEAT)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(
                s ->
                        new Object[] {
                            s.doubleAt(HEAT),
                            s.doubleAt(LEVEL) * 100,
                            s.doubleAt(TARGET) * 100,
                            s.intAt(X),
                            s.intAt(Y),
                            s.intAt(Z)
                        });
    }

    @LuaFunction(mainThread = true)
    public final Object[] setLevel(double level) {
        BE machine = machine();
        machine.setTarget(level / 100.0);
        machine.setChanged();
        return new Object[] {};
    }
}
