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

import com.hbm.blockentity.ordnance.launch.LaunchPlatformBlockEntity;
import com.hbm.compat.computercraft.FluidKeys;
import com.hbm.compat.computercraft.LuaEnums;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.interfaces.TriggerResult;
import com.hbm.items.weapon.MissileItem;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

public final class MissileLauncherPeripheral extends SnapshotPeripheral<LaunchPlatformBlockEntity> {

    private static final int POWER = 0;
    private static final int MAX_POWER = 1;
    private static final int FUEL = 2;
    private static final int FUEL_MAX = 3;
    private static final int OXIDIZER = 4;
    private static final int OXIDIZER_MAX = 5;
    private static final int READY = 6;
    private static final int TIER = 7;
    private static final int FUEL_KEY = 0;
    private static final int OXIDIZER_KEY = 1;

    public MissileLauncherPeripheral(LaunchPlatformBlockEntity machine) {
        super(machine, "ntm_launch_pad", 8, 2);
    }

    @Override
    protected void capture(LaunchPlatformBlockEntity machine) {
        put(POWER, machine.getPower());
        put(MAX_POWER, machine.getMaxPower());
        put(FUEL, machine.fuelTank.getFill());
        put(FUEL_MAX, machine.fuelTank.getMaxFill());
        putRef(FUEL_KEY, FluidKeys.of(machine.fuelTank.getTankType()));
        put(OXIDIZER, machine.oxidizerTank.getFill());
        put(OXIDIZER_MAX, machine.oxidizerTank.getMaxFill());
        putRef(OXIDIZER_KEY, FluidKeys.of(machine.oxidizerTank.getTankType()));
        put(READY, machine.launchReady());
        var missile = machine.getItem(LaunchPlatformBlockEntity.SLOT_MISSILE);
        put(
                TIER,
                machine.acceptsMissile(missile)
                        ? LuaEnums.missileTier(((MissileItem) missile.getItem()).tier)
                        : -1);
    }

    @LuaFunction
    public Object[] getEnergyInfo() {
        return read(s -> new Object[] {s.longAt(POWER), s.longAt(MAX_POWER)});
    }

    @LuaFunction
    public Object[] getFluid() {
        return read(
                s ->
                        new Object[] {
                            s.intAt(FUEL),
                            s.intAt(FUEL_MAX),
                            s.refAt(FUEL_KEY),
                            s.intAt(OXIDIZER),
                            s.intAt(OXIDIZER_MAX),
                            s.refAt(OXIDIZER_KEY)
                        });
    }

    @LuaFunction
    public Object[] canLaunch() {
        return new Object[] {booleanAt(READY)};
    }

    @LuaFunction
    public Object[] getTier() {
        var tier = intAt(TIER);
        return tier < 0 ? new Object[0] : new Object[] {tier};
    }

    @LuaFunction(mainThread = true)
    public Object[] launch(IArguments args) throws LuaException {
        var machine = machine();
        if (!machine.launchReady()) return new Object[] {false};
        var x = args.getInt(0);
        var z = args.getInt(1);
        return new Object[] {machine.launchAt(x, z) == TriggerResult.LAUNCHED};
    }

    @LuaFunction
    public Object[] getPos() {
        var pos = target.getBlockPos();
        return new Object[] {pos.getX(), pos.getY(), pos.getZ()};
    }
}
