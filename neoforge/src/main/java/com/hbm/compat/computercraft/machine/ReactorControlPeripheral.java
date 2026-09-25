// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.machine;

import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.BlockEntityReactorControl.RodFunction;
import com.hbm.tileentity.machine.BlockEntityReactorControl;
import dan200.computercraft.api.lua.LuaFunction;
import java.util.List;
import net.minecraft.util.Mth;

public final class ReactorControlPeripheral extends SnapshotPeripheral<BlockEntityReactorControl> {

    private static final int LINKED = 0;
    private static final int LEVEL = 1;
    private static final int FLUX = 2;
    private static final int HEAT = 3;
    private static final int FUNCTION = 4;
    private static final int HEAT_UPPER = 5;
    private static final int HEAT_LOWER = 6;
    private static final int LEVEL_UPPER = 7;
    private static final int LEVEL_LOWER = 8;

    public ReactorControlPeripheral(BlockEntityReactorControl machine) {
        super(machine, "reactor_control", 9, 0);
    }

    @Override
    protected void capture(BlockEntityReactorControl machine) {
        put(LINKED, machine.isLinked);
        put(LEVEL, machine.rodLevel);
        put(FLUX, machine.flux);
        put(HEAT, machine.heat);
        put(FUNCTION, machine.function.ordinal());
        put(HEAT_UPPER, machine.heatUpper);
        put(HEAT_LOWER, machine.heatLower);
        put(LEVEL_UPPER, machine.levelUpper);
        put(LEVEL_LOWER, machine.levelLower);
    }

    @LuaFunction
    public final Object[] isLinked() {
        return read(s -> new Object[] {s.booleanAt(LINKED)});
    }

    @LuaFunction
    public final Object[] getReactor() {
        return read(
                s ->
                        new Object[] {
                            s.booleanAt(LINKED)
                                    ? List.of(
                                            (int) (s.doubleAt(LEVEL) * 100),
                                            s.intAt(FLUX),
                                            (int) Math.round(s.intAt(HEAT) * 0.00002 * 980 + 20))
                                    : List.of(0, 0, 0)
                        });
    }

    @LuaFunction(mainThread = true)
    public final Object[] setParams(
            int function, double maxHeat, double minHeat, double maxLevel, double minLevel) {
        BlockEntityReactorControl machine = machine();
        machine.function = RodFunction.values()[Mth.clamp(function, 0, 2)];
        machine.heatUpper = Mth.clamp(maxHeat, 0, 9999);
        machine.heatLower = Mth.clamp(minHeat, 0, 9999);
        machine.levelUpper = Mth.clamp(maxLevel / 100.0, 0, 1);
        machine.levelLower = Mth.clamp(minLevel / 100.0, 0, 1);
        return new Object[] {};
    }

    @LuaFunction
    public final Object[] getParams() {
        return read(
                s ->
                        new Object[] {
                            s.intAt(FUNCTION),
                            s.doubleAt(HEAT_UPPER),
                            s.doubleAt(HEAT_LOWER),
                            s.doubleAt(LEVEL_UPPER),
                            s.doubleAt(LEVEL_LOWER)
                        });
    }
}
