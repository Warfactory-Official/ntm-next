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

import com.hbm.blockentity.machine.CombustionEngineBlockEntity;
import com.hbm.compat.computercraft.OpenComputers;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.trait.CombustibleTrait;
import com.hbm.items.PistonsItem;
import dan200.computercraft.api.lua.LuaFunction;

public final class CombustionGeneratorPeripheral
        extends SnapshotPeripheral<CombustionEngineBlockEntity> {

    private static final int FUEL = 0;
    private static final int FUEL_MAX = 1;
    private static final int POWER = 2;
    private static final int THROTTLE = 3;
    private static final int ON = 4;
    private static final int RATED = 5;
    private static final int EFFICIENCY = 6;
    private static final int FUEL_NAME = 0;
    private static final int MAX_THROTTLE = 30;

    public CombustionGeneratorPeripheral(CombustionEngineBlockEntity machine) {
        super(machine, "ntm_combustion_engine", 7, 1);
    }

    @Override
    protected void capture(CombustionEngineBlockEntity machine) {
        put(FUEL, machine.tank.getFill());
        put(FUEL_MAX, machine.tank.getMaxFill());
        putRef(FUEL_NAME, NTMFluids.legacyName(machine.tank.getTankType()));
        put(POWER, machine.power);
        put(THROTTLE, machine.setting);
        put(ON, machine.isOn);
        var piston = machine.getItem(CombustionEngineBlockEntity.SLOT_PISTON);
        var fuel = NTMFluidProperties.getTrait(machine.tank.getTankType(), CombustibleTrait.class);
        var rated = !piston.isEmpty() && fuel != null;
        put(RATED, rated);
        if (rated) {
            var kind =
                    piston.getItem() instanceof PistonsItem pistons
                            ? pistons.type
                            : PistonsItem.EnumPistonType.VALUES[0];
            put(EFFICIENCY, kind.eff[fuel.grade().ordinal()]);
        }
    }

    @LuaFunction
    public Object[] getFluid() {
        return read(s -> new Object[] {s.intAt(FUEL), s.intAt(FUEL_MAX)});
    }

    @LuaFunction("getType")
    public Object[] fuelName() {
        return new Object[] {refAt(FUEL_NAME)};
    }

    @LuaFunction
    public Object[] getPower() {
        return new Object[] {longAt(POWER)};
    }

    @LuaFunction
    public Object[] getThrottle() {
        return new Object[] {intAt(THROTTLE)};
    }

    @LuaFunction
    public Object[] getState() {
        return new Object[] {booleanAt(ON)};
    }

    @LuaFunction
    public Object[] getEfficiency() {
        return read(
                s ->
                        s.booleanAt(RATED)
                                ? new Object[] {s.doubleAt(EFFICIENCY)}
                                : OpenComputers.unknownError());
    }

    @LuaFunction(mainThread = true)
    public Object[] setThrottle(int throttle) {
        if (throttle < 0 || throttle > MAX_THROTTLE) {
            return new Object[] {false, "Throttle request outside of range 0-30"};
        }
        var machine = machine();
        if (machine.setting != throttle) {
            machine.hbm$syncSet$CombustionEngineBlockEntity$setting(throttle);
            machine.setChanged();
        }
        return new Object[] {true};
    }

    @LuaFunction(mainThread = true)
    public void start() {
        switchTo(true);
    }

    @LuaFunction(mainThread = true)
    public void stop() {
        switchTo(false);
    }

    private void switchTo(boolean on) {
        var machine = machine();
        if (machine.isOn == on) return;
        machine.hbm$syncSet$CombustionEngineBlockEntity$isOn(on);
        machine.setChanged();
    }

    @LuaFunction
    public Object[] getInfo() {
        return read(
                s ->
                        s.booleanAt(RATED)
                                ? new Object[] {
                                    s.intAt(THROTTLE),
                                    s.booleanAt(ON),
                                    s.longAt(POWER),
                                    s.doubleAt(EFFICIENCY),
                                    s.intAt(FUEL),
                                    s.intAt(FUEL_MAX),
                                    s.refAt(FUEL_NAME)
                                }
                                : OpenComputers.unknownError());
    }
}
