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

import com.hbm.compat.computercraft.BlockEntityPeripheral;
import com.hbm.compat.computercraft.SteamTypes;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.level.block.entity.BlockEntity;

public abstract class TurbinePeripheral<BE extends BlockEntity> extends BlockEntityPeripheral<BE> {

    protected TurbinePeripheral(BE machine) {
        super(machine, "ntm_turbine");
    }

    protected abstract FluidTankNTM input(BE machine);

    protected abstract FluidTankNTM output(BE machine);

    protected abstract long power(BE machine);

    @LuaFunction
    public final Object[] getFluid() {
        return new Object[] {
            (int) input(target).getFill(),
            (int) input(target).getMaxFill(),
            (int) output(target).getFill(),
            (int) output(target).getMaxFill()
        };
    }

    @LuaFunction("getType")
    public final Object[] steamType() {
        return new Object[] {(int) SteamTypes.toInt(input(target).getTankType())};
    }

    @LuaFunction
    public final Object[] getPower() {
        return new Object[] {(long) power(target)};
    }
}
