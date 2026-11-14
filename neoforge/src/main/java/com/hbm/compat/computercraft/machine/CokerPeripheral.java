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

import com.hbm.blockentity.machine.oil.CokerBlockEntity;
import com.hbm.compat.computercraft.BlockEntityPeripheral;
import com.hbm.inventory.fluid.NTMFluids;
import dan200.computercraft.api.lua.LuaFunction;

public final class CokerPeripheral extends BlockEntityPeripheral<CokerBlockEntity> {

    public CokerPeripheral(CokerBlockEntity machine) {
        super(machine, "ntm_coker");
    }

    @LuaFunction
    public Object[] getTypeStored() {
        return new Object[] {
            NTMFluids.legacyName(target.tanks[0].getTankType()),
            NTMFluids.legacyName(target.tanks[1].getTankType())
        };
    }

    @LuaFunction
    public Object[] getFluidStored() {
        return new Object[] {(int) target.tanks[0].getFill(), (int) target.tanks[1].getFill()};
    }

    @LuaFunction
    public Object[] getHeat() {
        return new Object[] {(int) target.heat};
    }

    @LuaFunction
    public Object[] getInfo() {
        return new Object[] {
            NTMFluids.legacyName(target.tanks[0].getTankType()),
            NTMFluids.legacyName(target.tanks[1].getTankType()),
            (int) target.tanks[0].getFill(),
            (int) target.tanks[1].getFill(),
            (int) target.heat
        };
    }
}
