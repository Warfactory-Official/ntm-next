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

package com.hbm.compat.computercraft.fusion;

import com.hbm.blockentity.machine.fusion.FusionMHDTBlockEntity;
import com.hbm.compat.computercraft.BlockEntityPeripheral;
import dan200.computercraft.api.lua.LuaFunction;

public final class FusionMHDTPeripheral extends BlockEntityPeripheral<FusionMHDTBlockEntity> {

    public FusionMHDTPeripheral(FusionMHDTBlockEntity machine) {
        super(machine, "ntm_fusion_mhdt");
    }

    @LuaFunction
    public Object[] getEnergyInfo() {
        return new Object[] {(long) target.power};
    }

    @LuaFunction
    public Object[] getPlasmaEnergy() {
        return new Object[] {(long) target.plasmaEnergySync};
    }

    @LuaFunction
    public Object[] getCoolant() {
        return new Object[] {
            (int) target.tanks[0].getFill(),
            (int) target.tanks[0].getMaxFill(),
            (int) target.tanks[1].getFill(),
            (int) target.tanks[1].getMaxFill()
        };
    }

    @LuaFunction
    public Object[] getInfo() {
        return new Object[] {
            (long) target.power,
            (long) target.plasmaEnergySync,
            (int) target.tanks[0].getFill(),
            (int) target.tanks[0].getMaxFill(),
            (int) target.tanks[1].getFill(),
            (int) target.tanks[1].getMaxFill()
        };
    }
}
