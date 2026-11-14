/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.compat.computercraft.machine;

import com.hbm.blockentity.machine.ReactorZirnoxBlockEntity;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.items.machine.ZirnoxRodItem;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

public final class ZirnoxReactorPeripheral extends SnapshotPeripheral<ReactorZirnoxBlockEntity> {

    private static final int TEMPERATURE = 0;
    private static final int PRESSURE = 1;
    private static final int WATER = 2;
    private static final int STEAM = 3;
    private static final int CARBON_DIOXIDE = 4;
    private static final int ON = 5;
    private static final int FUEL_SLOTS = 24;
    private static final int DEFAULT_VENT = 1000;

    public ZirnoxReactorPeripheral(ReactorZirnoxBlockEntity machine) {
        super(machine, "zirnox_reactor", 6, 0);
    }

    @Override
    protected void capture(ReactorZirnoxBlockEntity machine) {
        put(TEMPERATURE, Math.round(machine.heat * 0.00001 * 780 + 20));
        put(PRESSURE, Math.round(machine.pressure * 0.00001 * 30));
        put(WATER, machine.water.getFill());
        put(STEAM, machine.steam.getFill());
        put(CARBON_DIOXIDE, machine.carbonDioxide.getFill());
        put(ON, machine.isOn);
    }

    @LuaFunction
    public Object[] getTemp() {
        return new Object[] {longAt(TEMPERATURE)};
    }

    @LuaFunction
    public Object[] getPressure() {
        return new Object[] {longAt(PRESSURE)};
    }

    @LuaFunction
    public Object[] getWater() {
        return new Object[] {intAt(WATER)};
    }

    @LuaFunction
    public Object[] getSteam() {
        return new Object[] {intAt(STEAM)};
    }

    @LuaFunction
    public Object[] getCarbonDioxide() {
        return new Object[] {intAt(CARBON_DIOXIDE)};
    }

    @LuaFunction
    public Object[] isActive() {
        return new Object[] {booleanAt(ON)};
    }

    @LuaFunction
    public Object[] getInfo() {
        return read(
                s ->
                        new Object[] {
                            s.longAt(TEMPERATURE),
                            s.longAt(PRESSURE),
                            s.intAt(WATER),
                            s.intAt(STEAM),
                            s.intAt(CARBON_DIOXIDE),
                            s.booleanAt(ON)
                        });
    }

    @LuaFunction(mainThread = true)
    public Object[] getFuel(int slot) {
        if (slot >= 0 && slot < FUEL_SLOTS) {
            var stack = machine().getItem(slot);
            if (stack.getItem() instanceof ZirnoxRodItem rod && !rod.type.breeding) {
                return new Object[] {
                    rod.type.legacyName(),
                    ZirnoxRodItem.getLifeTime(stack),
                    rod.type.maxLife,
                    0,
                    false
                };
            }
        }
        return new Object[] {"", 0, 0, 0, false};
    }

    @LuaFunction(mainThread = true)
    public void setActive(boolean on) {
        var machine = machine();
        if (machine.isOn == on) return;
        machine.hbm$syncSet$ReactorZirnoxBlockEntity$isOn(on);
        machine.setChanged();
    }

    @LuaFunction(mainThread = true)
    public void ventCarbonDioxide(IArguments args) throws LuaException {
        var machine = machine();
        var tank = machine.carbonDioxide;
        var vented = Math.clamp(args.optInt(0, DEFAULT_VENT), 0, tank.getMaxFill());
        var fill = Math.max(tank.getFill() - vented, 0);
        if (fill == tank.getFill()) return;
        tank.setFill(fill);
        machine.setChanged();
    }
}
