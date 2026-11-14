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

package com.hbm.compat.computercraft.turret;

import com.hbm.blockentity.turret.TurretBaseNTBlockEntity;
import com.hbm.compat.computercraft.OpenComputers;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.items.ModItems;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

public class TurretControlPeripheral<BE extends TurretBaseNTBlockEntity>
        extends SnapshotPeripheral<BE> {

    private static final int ON = 0;
    private static final int POWER = 1;
    private static final int MAX_POWER = 2;
    private static final int PLAYERS = 3;
    private static final int ANIMALS = 4;
    private static final int MOBS = 5;
    private static final int MACHINES = 6;
    private static final int HAS_TARGET = 7;
    private static final int PITCH = 8;
    private static final int YAW = 9;
    private static final int ALIGNED = 10;
    protected static final int CORE_X = 11;
    protected static final int CORE_Y = 12;
    protected static final int CORE_Z = 13;
    protected static final int TURRET_NUMBERS = 14;

    public TurretControlPeripheral(BE machine) {
        this(machine, "ntm_turret", TURRET_NUMBERS, 0);
    }

    protected TurretControlPeripheral(BE machine, String type, int numbers, int texts) {
        super(machine, type, numbers, texts);
    }

    @Override
    protected void capture(BE machine) {
        put(ON, machine.isOn);
        put(POWER, machine.getPower());
        put(MAX_POWER, machine.getMaxPower());
        put(PLAYERS, machine.targetPlayers);
        put(ANIMALS, machine.targetAnimals);
        put(MOBS, machine.targetMobs);
        put(MACHINES, machine.targetMachines);
        put(HAS_TARGET, machine.target != null);
        put(PITCH, Math.toDegrees(machine.rotationPitch));
        put(YAW, Math.toDegrees(machine.rotationYaw));
        put(ALIGNED, machine.aligned);
        var core = machine.getBlockPos();
        put(CORE_X, core.getX());
        put(CORE_Y, core.getY());
        put(CORE_Z, core.getZ());
    }

    @LuaFunction(mainThread = true)
    public final void setActive(boolean on) {
        var machine = machine();
        if (machine.isOn == on) return;
        machine.hbm$syncSet$TurretBaseNTBlockEntity$isOn(on);
        machine.markChanged();
    }

    @LuaFunction
    public final Object[] isActive() {
        return new Object[] {booleanAt(ON)};
    }

    @LuaFunction
    public final Object[] getEnergyInfo() {
        return read(s -> new Object[] {s.longAt(POWER), s.longAt(MAX_POWER)});
    }

    @LuaFunction(mainThread = true)
    public final Object[] getWhitelisted() {
        var machine = machine();
        if (!machine.getItem(TurretBaseNTBlockEntity.SLOT_CHIP).is(ModItems.TURRET_CHIP.get()))
            return new Object[0];
        var whitelist = machine.getWhitelist();
        return new Object[] {whitelist == null ? null : whitelist.toArray()};
    }

    @LuaFunction(mainThread = true)
    public final Object[] addWhitelist(String name) {
        var machine = machine();
        var whitelist = machine.getWhitelist();
        if (whitelist != null && whitelist.contains(name)) return new Object[] {false};
        machine.addName(name);
        return new Object[] {true};
    }

    @LuaFunction(mainThread = true)
    public final Object[] removeWhitelist(IArguments args) throws LuaException {
        var machine = machine();
        var whitelist = machine.getWhitelist();
        if (whitelist == null) return OpenComputers.unknownError();
        var index = whitelist.indexOf(args.getString(0));
        if (index < 0) return new Object[] {false};
        machine.removeName(index);
        return new Object[] {true};
    }

    @LuaFunction(mainThread = true)
    public final void setTargeting(
            boolean players, boolean animals, boolean mobs, boolean machines) {
        var machine = machine();
        if (machine.targetPlayers == players
                && machine.targetAnimals == animals
                && machine.targetMobs == mobs
                && machine.targetMachines == machines) {
            return;
        }
        machine.hbm$syncSet$TurretBaseNTBlockEntity$targetPlayers(players);
        machine.hbm$syncSet$TurretBaseNTBlockEntity$targetAnimals(animals);
        machine.hbm$syncSet$TurretBaseNTBlockEntity$targetMobs(mobs);
        machine.hbm$syncSet$TurretBaseNTBlockEntity$targetMachines(machines);
        machine.markChanged();
    }

    @LuaFunction
    public final Object[] getTargeting() {
        return read(
                s ->
                        new Object[] {
                            s.booleanAt(PLAYERS),
                            s.booleanAt(ANIMALS),
                            s.booleanAt(MOBS),
                            s.booleanAt(MACHINES)
                        });
    }

    @LuaFunction
    public final Object[] hasTarget() {
        return new Object[] {booleanAt(HAS_TARGET)};
    }

    @LuaFunction
    public final Object[] getAngle() {
        return read(s -> new Object[] {s.doubleAt(PITCH), s.doubleAt(YAW)});
    }

    @LuaFunction
    public final Object[] isAligned() {
        return new Object[] {booleanAt(ALIGNED)};
    }

    @LuaFunction
    public final Object[] getPos() {
        return read(s -> new Object[] {s.intAt(CORE_X), s.intAt(CORE_Y), s.intAt(CORE_Z)});
    }
}
