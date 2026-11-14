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

import com.hbm.blockentity.machine.IndustrialTurbineBlockEntity;
import com.hbm.compat.computercraft.SteamTypes;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import dan200.computercraft.api.lua.LuaFunction;

public final class IndustrialTurbinePeripheral
        extends TurbinePeripheral<IndustrialTurbineBlockEntity> {

    public IndustrialTurbinePeripheral(IndustrialTurbineBlockEntity machine) {
        super(machine);
    }

    @Override
    protected FluidTankNTM input(IndustrialTurbineBlockEntity machine) {
        return machine.tanks[0];
    }

    @Override
    protected FluidTankNTM output(IndustrialTurbineBlockEntity machine) {
        return machine.tanks[1];
    }

    @Override
    protected long power(IndustrialTurbineBlockEntity machine) {
        return machine.powerBuffer;
    }

    @LuaFunction
    public Object[] getFlywheel() {
        return new Object[] {(int) (target.spin * 100)};
    }

    @LuaFunction
    public Object[] getInfo() {
        return new Object[] {
            (int) input(target).getFill(),
            (int) input(target).getMaxFill(),
            (int) output(target).getFill(),
            (int) output(target).getMaxFill(),
            (int) SteamTypes.toInt(input(target).getTankType()),
            (long) power(target),
            (int) (target.spin * 100)
        };
    }
}
