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

package com.hbm.compat.computercraft.bomb;

import com.hbm.api.item.TargetDesignator;
import com.hbm.blockentity.ordnance.launch.LaunchTableBlockEntity;
import com.hbm.compat.computercraft.FluidKeys;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.items.ModDataComponents;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

public final class CustomMissileLauncherPeripheral
        extends SnapshotPeripheral<LaunchTableBlockEntity> {

    private static final int POWER = 0;
    private static final int MAX_POWER = 1;
    private static final int FUEL = 2;
    private static final int FUEL_MAX = 3;
    private static final int OXIDIZER = 4;
    private static final int OXIDIZER_MAX = 5;
    private static final int SOLID = 6;
    private static final int CAN_LAUNCH = 7;
    private static final int MISSILE_VALID = 8;
    private static final int HAS_DESIGNATOR = 9;
    private static final int HAS_FUEL = 10;
    private static final int DESIGNATOR = 11;
    private static final int TARGET_X = 12;
    private static final int TARGET_Z = 13;
    private static final int FUEL_KEY = 0;
    private static final int OXIDIZER_KEY = 1;

    private static final int NO_DESIGNATOR = 0;
    private static final int NO_TARGET = 1;
    private static final int TARGET = 2;
    private static final String MISSING = "Designator not found";

    public CustomMissileLauncherPeripheral(LaunchTableBlockEntity machine) {
        super(machine, "ntm_custom_launch_pad", 14, 2);
    }

    @Override
    protected void capture(LaunchTableBlockEntity machine) {
        put(POWER, machine.getPower());
        put(MAX_POWER, machine.getMaxPower());
        put(FUEL, machine.propellantTank.getFill());
        put(FUEL_MAX, machine.propellantTank.getMaxFill());
        putRef(FUEL_KEY, FluidKeys.of(machine.propellantTank.getTankType()));
        put(OXIDIZER, machine.oxidizerTank.getFill());
        put(OXIDIZER_MAX, machine.oxidizerTank.getMaxFill());
        putRef(OXIDIZER_KEY, FluidKeys.of(machine.oxidizerTank.getTankType()));
        put(SOLID, machine.solidPropellant);
        put(CAN_LAUNCH, machine.launchReady());
        put(MISSILE_VALID, machine.acceptsAssembly());
        put(HAS_DESIGNATOR, machine.designatorReady());
        put(HAS_FUEL, machine.propellantReady());
        var designator = machine.getItem(LaunchTableBlockEntity.DESIGNATOR_SLOT);
        var target = designator.get(ModDataComponents.TARGET_DESIGNATOR.get());
        if (!(designator.getItem() instanceof TargetDesignator)) {
            put(DESIGNATOR, NO_DESIGNATOR);
        } else if (target == null) {
            put(DESIGNATOR, NO_TARGET);
        } else {
            put(DESIGNATOR, TARGET);
            put(TARGET_X, TargetDesignator.unpackX(target));
            put(TARGET_Z, TargetDesignator.unpackZ(target));
        }
    }

    @LuaFunction
    public Object[] getEnergyInfo() {
        return read(s -> new Object[] {s.longAt(POWER), s.longAt(MAX_POWER)});
    }

    @LuaFunction
    public Object[] getContents() {
        return read(
                s ->
                        new Object[] {
                            s.intAt(FUEL),
                            s.intAt(FUEL_MAX),
                            s.refAt(FUEL_KEY),
                            s.intAt(OXIDIZER),
                            s.intAt(OXIDIZER_MAX),
                            s.refAt(OXIDIZER_KEY),
                            s.intAt(SOLID),
                            LaunchTableBlockEntity.SOLID_CAPACITY
                        });
    }

    @LuaFunction
    public Object[] getLaunchInfo() {
        return read(
                s ->
                        new Object[] {
                            s.booleanAt(CAN_LAUNCH),
                            s.booleanAt(MISSILE_VALID),
                            s.booleanAt(HAS_DESIGNATOR),
                            s.booleanAt(HAS_FUEL)
                        });
    }

    @LuaFunction
    public Object[] getCoords() {
        return read(
                s ->
                        switch (s.intAt(DESIGNATOR)) {
                            case NO_DESIGNATOR -> new Object[] {false, MISSING};
                            case NO_TARGET -> new Object[] {false};
                            default -> new Object[] {s.intAt(TARGET_X), s.intAt(TARGET_Z)};
                        });
    }

    @LuaFunction(mainThread = true)
    public Object[] setCoords(IArguments args) throws LuaException {
        var machine = machine();
        var designator = machine.getItem(LaunchTableBlockEntity.DESIGNATOR_SLOT);
        if (!(designator.getItem() instanceof TargetDesignator))
            return new Object[] {false, MISSING};
        var target = TargetDesignator.pack(args.getInt(0), args.getInt(1));
        var component = ModDataComponents.TARGET_DESIGNATOR.get();
        var current = designator.get(component);
        if (current == null || current != target) {
            designator.set(component, target);
            machine.setChanged();
        }
        return new Object[] {true};
    }

    @LuaFunction(mainThread = true)
    public Object[] launch() {
        var machine = machine();
        if (!machine.launchReady()) return new Object[] {false};
        machine.launchDesignated();
        return new Object[] {true};
    }
}
