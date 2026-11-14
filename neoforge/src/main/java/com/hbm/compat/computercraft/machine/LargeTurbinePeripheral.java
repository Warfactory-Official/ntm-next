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

import com.hbm.blockentity.machine.LargeTurbineBlockEntity;
import com.hbm.compat.computercraft.SteamTypes;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import dan200.computercraft.api.lua.LuaFunction;

public final class LargeTurbinePeripheral extends TurbinePeripheral<LargeTurbineBlockEntity> {

    public LargeTurbinePeripheral(LargeTurbineBlockEntity machine) {
        super(machine);
    }

    @Override
    protected FluidTankNTM input(LargeTurbineBlockEntity machine) {
        return machine.tanks[0];
    }

    @Override
    protected FluidTankNTM output(LargeTurbineBlockEntity machine) {
        return machine.tanks[1];
    }

    @Override
    protected long power(LargeTurbineBlockEntity machine) {
        return machine.power;
    }

    @LuaFunction(mainThread = true)
    public Object[] setType(int type) {
        LargeTurbineBlockEntity machine = machine();
        machine.tanks[0].setTankType(SteamTypes.fromInt(type));
        machine.markChanged();
        return new Object[] {};
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
