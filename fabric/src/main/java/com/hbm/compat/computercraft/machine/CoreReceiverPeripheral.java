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

import com.hbm.blockentity.machine.dfc.DfcReceiverBlockEntity;
import com.hbm.compat.computercraft.BlockEntityPeripheral;
import dan200.computercraft.api.lua.LuaFunction;

public final class CoreReceiverPeripheral extends BlockEntityPeripheral<DfcReceiverBlockEntity> {

    public CoreReceiverPeripheral(DfcReceiverBlockEntity machine) {
        super(machine, "dfc_receiver");
    }

    @LuaFunction
    public Object[] getEnergyInfo() {
        return new Object[] {(long) target.joules, (long) target.getPower()};
    }

    @LuaFunction
    public Object[] getCryogel() {
        return new Object[] {(int) target.tank.getFill()};
    }

    @LuaFunction
    public Object[] getInfo() {
        return new Object[] {
            (long) target.joules, (long) target.getPower(), (int) target.tank.getFill()
        };
    }
}
