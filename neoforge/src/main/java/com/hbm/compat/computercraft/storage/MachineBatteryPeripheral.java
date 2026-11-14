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

import com.hbm.blockentity.machine.storage.BatteryBlockEntity;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import dan200.computercraft.api.lua.LuaFunction;

public final class MachineBatteryPeripheral extends SnapshotPeripheral<BatteryBlockEntity> {

    private static final int POWER = 0;
    private static final int MAX_POWER = 1;

    public MachineBatteryPeripheral(BatteryBlockEntity machine) {
        super(machine, "ntm_energy_storage_legacy", 2, 0);
    }

    @Override
    protected void capture(BatteryBlockEntity machine) {
        put(POWER, machine.getPower());
        put(MAX_POWER, machine.getMaxPower());
    }

    @LuaFunction
    public Object[] getEnergyInfo() {
        return read(s -> new Object[] {s.longAt(POWER), s.longAt(MAX_POWER)});
    }

    @LuaFunction
    public Object[] getInfo() {
        return read(s -> new Object[] {s.longAt(POWER), s.longAt(MAX_POWER)});
    }
}
