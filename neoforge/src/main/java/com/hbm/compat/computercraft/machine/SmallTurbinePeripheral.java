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

import com.hbm.blockentity.machine.thermal.CompactTurbineBlockEntity;
import com.hbm.compat.computercraft.SteamTypes;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import dan200.computercraft.api.lua.LuaFunction;

public final class SmallTurbinePeripheral extends TurbinePeripheral<CompactTurbineBlockEntity> {

    public SmallTurbinePeripheral(CompactTurbineBlockEntity machine) {
        super(machine);
    }

    @Override
    protected FluidTankNTM input(CompactTurbineBlockEntity machine) {
        return machine.tank0;
    }

    @Override
    protected FluidTankNTM output(CompactTurbineBlockEntity machine) {
        return machine.tank1;
    }

    @Override
    protected long power(CompactTurbineBlockEntity machine) {
        return machine.power;
    }

    @LuaFunction(mainThread = true)
    public Object[] setType(int type) {
        CompactTurbineBlockEntity machine = machine();
        machine.tank0.setTankType(SteamTypes.fromInt(type));
        machine.markChanged();
        return new Object[] {true};
    }

    @LuaFunction
    public Object[] getInfo() {
        return new Object[] {
            (int) input(target).getFill(),
            (int) input(target).getMaxFill(),
            (int) output(target).getFill(),
            (int) output(target).getMaxFill(),
            (int) SteamTypes.toInt(input(target).getTankType()),
            (long) power(target)
        };
    }
}
