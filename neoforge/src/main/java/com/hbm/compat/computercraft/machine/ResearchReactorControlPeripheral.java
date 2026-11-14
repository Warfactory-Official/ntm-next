/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.compat.computercraft.machine;

import com.hbm.blockentity.machine.reactor.ReactorControlBlockEntity;
import com.hbm.compat.computercraft.LuaEnums;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import dan200.computercraft.api.lua.LuaFunction;

public final class ResearchReactorControlPeripheral
        extends SnapshotPeripheral<ReactorControlBlockEntity> {

    private static final int LINKED = 0;
    private static final int ROD_LEVEL = 1;
    private static final int FLUX = 2;
    private static final int HEAT = 3;
    private static final int CURVE = 4;
    private static final int HEAT_UPPER = 5;
    private static final int HEAT_LOWER = 6;
    private static final int LEVEL_UPPER = 7;
    private static final int LEVEL_LOWER = 8;
    private static final double MAX_HEAT = 9999D;

    public ResearchReactorControlPeripheral(ReactorControlBlockEntity machine) {
        super(machine, "reactor_control", 9, 0);
    }

    @Override
    protected void capture(ReactorControlBlockEntity machine) {
        put(LINKED, machine.linked);
        put(ROD_LEVEL, machine.actualRodPosition);
        put(FLUX, machine.flux);
        put(HEAT, machine.internalHeat);
        put(CURVE, LuaEnums.curveIndex(machine.curve));
        put(HEAT_UPPER, machine.upperHeat);
        put(HEAT_LOWER, machine.lowerHeat);
        put(LEVEL_UPPER, machine.upperRodPercent);
        put(LEVEL_LOWER, machine.lowerRodPercent);
    }

    @LuaFunction
    public Object[] isLinked() {
        return new Object[] {booleanAt(LINKED)};
    }

    @LuaFunction
    public Object[] getReactor() {
        return read(
                s ->
                        new Object[] {
                            s.booleanAt(LINKED)
                                    ? new Object[] {
                                        (int) (s.doubleAt(ROD_LEVEL) * 100),
                                        s.intAt(FLUX),
                                        (int) Math.round(s.intAt(HEAT) * 0.00002 * 980 + 20)
                                    }
                                    : new Object[] {0, 0, 0}
                        });
    }

    @LuaFunction(mainThread = true)
    public void setParams(
            int curve, double maxHeat, double minHeat, double maxLevel, double minLevel) {
        var function = LuaEnums.curve(Math.clamp(curve, 0, 2));
        var heatUpper = Math.clamp(maxHeat, 0D, MAX_HEAT);
        var heatLower = Math.clamp(minHeat, 0D, MAX_HEAT);
        var levelUpper = Math.clamp(maxLevel / 100D, 0D, 1D);
        var levelLower = Math.clamp(minLevel / 100D, 0D, 1D);
        var machine = machine();
        if (machine.curve == function
                && machine.upperHeat == heatUpper
                && machine.lowerHeat == heatLower
                && machine.upperRodPercent == levelUpper
                && machine.lowerRodPercent == levelLower) {
            return;
        }
        machine.hbm$syncSet$ReactorControlBlockEntity$curve(function);
        machine.hbm$syncSet$ReactorControlBlockEntity$upperHeat(heatUpper);
        machine.hbm$syncSet$ReactorControlBlockEntity$lowerHeat(heatLower);
        machine.hbm$syncSet$ReactorControlBlockEntity$upperRodPercent(levelUpper);
        machine.hbm$syncSet$ReactorControlBlockEntity$lowerRodPercent(levelLower);
        machine.setChanged();
    }

    @LuaFunction
    public Object[] getParams() {
        return read(
                s ->
                        new Object[] {
                            s.intAt(CURVE),
                            s.doubleAt(HEAT_UPPER),
                            s.doubleAt(HEAT_LOWER),
                            s.doubleAt(LEVEL_UPPER),
                            s.doubleAt(LEVEL_LOWER)
                        });
    }
}
