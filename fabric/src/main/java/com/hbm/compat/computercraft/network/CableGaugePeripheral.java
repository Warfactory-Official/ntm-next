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

package com.hbm.compat.computercraft.network;

import com.hbm.blockentity.network.CableGaugeBlockEntity;
import com.hbm.compat.computercraft.BlockEntityPeripheral;
import dan200.computercraft.api.lua.LuaFunction;

public final class CableGaugePeripheral extends BlockEntityPeripheral<CableGaugeBlockEntity> {

    public CableGaugePeripheral(CableGaugeBlockEntity machine) {
        super(machine, "ntm_power_gauge");
    }

    @LuaFunction
    public Object[] getTransfer() {
        return new Object[] {
            (long) target.getTransferredThisTick(), (long) target.getTransferredLastInterval()
        };
    }

    @LuaFunction
    public Object[] getInfo() {
        return new Object[] {
            (long) target.getTransferredThisTick(),
            (long) target.getTransferredLastInterval(),
            (int) target.getBlockPos().getX(),
            (int) target.getBlockPos().getY(),
            (int) target.getBlockPos().getZ()
        };
    }
}
