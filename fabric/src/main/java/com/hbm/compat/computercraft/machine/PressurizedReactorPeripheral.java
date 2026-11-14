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

import com.hbm.blockentity.machine.PWRControllerBlockEntity;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import dan200.computercraft.api.lua.LuaFunction;

public final class PressurizedReactorPeripheral
        extends SnapshotPeripheral<PWRControllerBlockEntity> {

    private static final int CORE_HEAT = 0;
    private static final int HULL_HEAT = 1;
    private static final int CORE_CAPACITY = 2;
    private static final int FLUX = 3;
    private static final int ROD_TARGET = 4;
    private static final int ROD_LEVEL = 5;
    private static final int LOADED = 6;
    private static final int PROGRESS = 7;
    private static final int PROCESS_TIME = 8;
    private static final int COOLANT = 9;
    private static final int COOLANT_MAX = 10;
    private static final int HOT = 11;
    private static final int HOT_MAX = 12;

    public PressurizedReactorPeripheral(PWRControllerBlockEntity machine) {
        super(machine, "ntm_pwr_control", 13, 0);
    }

    @Override
    protected void capture(PWRControllerBlockEntity machine) {
        put(CORE_HEAT, machine.coreHeat);
        put(HULL_HEAT, machine.hullHeat);
        put(CORE_CAPACITY, machine.coreHeatCapacity);
        put(FLUX, machine.flux);
        put(ROD_TARGET, machine.rodTarget);
        put(ROD_LEVEL, machine.rodLevel);
        put(LOADED, machine.amountLoaded);
        put(PROGRESS, machine.progress);
        put(PROCESS_TIME, machine.processTime);
        put(COOLANT, machine.tanks[0].getFill());
        put(COOLANT_MAX, machine.tanks[0].getMaxFill());
        put(HOT, machine.tanks[1].getFill());
        put(HOT_MAX, machine.tanks[1].getMaxFill());
    }

    @LuaFunction
    public Object[] getHeat() {
        return read(
                s ->
                        new Object[] {
                            s.longAt(CORE_HEAT),
                            s.longAt(HULL_HEAT),
                            s.longAt(CORE_CAPACITY),
                            PWRControllerBlockEntity.hullHeatCapacityBase
                        });
    }

    @LuaFunction
    public Object[] getFlux() {
        return new Object[] {doubleAt(FLUX)};
    }

    @LuaFunction
    public Object[] getLevel() {
        return read(s -> new Object[] {s.doubleAt(ROD_TARGET), s.doubleAt(ROD_LEVEL)});
    }

    @LuaFunction
    public Object[] getCoolantInfo() {
        return read(
                s ->
                        new Object[] {
                            s.intAt(COOLANT), s.intAt(COOLANT_MAX), s.intAt(HOT), s.intAt(HOT_MAX)
                        });
    }

    @LuaFunction
    public Object[] getFuelInfo() {
        return read(
                s ->
                        new Object[] {
                            s.intAt(LOADED), s.doubleAt(PROGRESS), s.doubleAt(PROCESS_TIME)
                        });
    }

    @LuaFunction
    public Object[] getInfo() {
        return read(
                s ->
                        new Object[] {
                            s.longAt(CORE_HEAT),
                            s.longAt(HULL_HEAT),
                            s.longAt(CORE_CAPACITY),
                            PWRControllerBlockEntity.hullHeatCapacityBase,
                            s.doubleAt(FLUX),
                            s.doubleAt(ROD_TARGET),
                            s.doubleAt(ROD_LEVEL),
                            s.intAt(LOADED),
                            s.doubleAt(PROGRESS),
                            s.doubleAt(PROCESS_TIME),
                            s.intAt(COOLANT),
                            s.intAt(COOLANT_MAX),
                            s.intAt(HOT),
                            s.intAt(HOT_MAX)
                        });
    }

    @LuaFunction(mainThread = true)
    public Object[] setLevel(double level) {
        var inserted = Math.clamp(level, 0D, 100D);
        var machine = machine();
        if (machine.rodTarget != inserted) {
            machine.hbm$syncSet$PWRControllerBlockEntity$rodTarget(inserted);
            machine.markChanged();
        }
        return new Object[] {true};
    }
}
