// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.machine;

import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.BlockEntityReactorResearch;
import dan200.computercraft.api.lua.LuaFunction;

public final class ResearchReactorPeripheral
        extends SnapshotPeripheral<BlockEntityReactorResearch> {

    private static final int HEAT = 0;
    private static final int LEVEL = 1;
    private static final int TARGET = 2;
    private static final int FLUX = 3;

    public ResearchReactorPeripheral(BlockEntityReactorResearch machine) {
        super(machine, "research_reactor", 4, 0);
    }

    @Override
    protected void capture(BlockEntityReactorResearch machine) {
        put(HEAT, machine.heat);
        put(LEVEL, machine.controlLevel);
        put(TARGET, machine.targetLevel);
        put(FLUX, machine.totalFlux);
    }

    @LuaFunction
    public final Object[] getTemp() {
        return read(s -> new Object[] {s.intAt(HEAT)});
    }

    @LuaFunction
    public final Object[] getLevel() {
        return read(s -> new Object[] {s.doubleAt(LEVEL) * 100});
    }

    @LuaFunction
    public final Object[] getTargetLevel() {
        return read(s -> new Object[] {s.doubleAt(TARGET)});
    }

    @LuaFunction
    public final Object[] getFlux() {
        return read(s -> new Object[] {s.intAt(FLUX)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(
                s ->
                        new Object[] {
                            s.intAt(HEAT), s.doubleAt(LEVEL), s.doubleAt(TARGET), s.intAt(FLUX)
                        });
    }

    @LuaFunction(mainThread = true)
    public final Object[] setLevel(double level) {
        BlockEntityReactorResearch machine = machine();
        machine.targetLevel = Math.clamp(level / 100.0, 0, 1.0);
        machine.setChanged();
        return new Object[] {};
    }
}
