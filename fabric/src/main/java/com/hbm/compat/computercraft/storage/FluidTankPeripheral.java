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

package com.hbm.compat.computercraft.storage;

import com.hbm.compat.computercraft.BlockEntityPeripheral;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.level.block.entity.BlockEntity;

public abstract class FluidTankPeripheral<BE extends BlockEntity>
        extends BlockEntityPeripheral<BE> {

    protected FluidTankPeripheral(BE machine) {
        super(machine, "ntm_fluid_tank");
    }

    protected abstract FluidTankNTM tank(BE machine);

    @LuaFunction
    public final Object[] getFluidStored() {
        return new Object[] {(int) tank(target).getFill()};
    }

    @LuaFunction
    public final Object[] getMaxStored() {
        return new Object[] {(int) tank(target).getMaxFill()};
    }

    @LuaFunction
    public final Object[] getTypeStored() {
        return new Object[] {NTMFluids.legacyName(tank(target).getTankType())};
    }

    @LuaFunction
    public final Object[] getInfo() {
        return new Object[] {
            (int) tank(target).getFill(),
            (int) tank(target).getMaxFill(),
            NTMFluids.legacyName(tank(target).getTankType())
        };
    }
}
