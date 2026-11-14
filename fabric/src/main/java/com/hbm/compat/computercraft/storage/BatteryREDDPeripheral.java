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

import com.hbm.blockentity.machine.storage.ReddBlockEntity;
import dan200.computercraft.api.lua.LuaFunction;

public final class BatteryREDDPeripheral extends StorageModesPeripheral<ReddBlockEntity> {

    private static final int POWER = MODE_NUMBERS;
    private static final int DELTA = MODE_NUMBERS + 1;

    public BatteryREDDPeripheral(ReddBlockEntity machine) {
        super(machine, MODE_NUMBERS + 2, 0);
    }

    @Override
    protected void capture(ReddBlockEntity machine) {
        super.capture(machine);
        put(POWER, machine.getExactStock().doubleValue());
        put(DELTA, machine.getExactDelta().longValue());
    }

    @LuaFunction
    public Object[] getEnergyInfo() {
        return read(s -> new Object[] {s.doubleAt(POWER), s.longAt(DELTA)});
    }

    @LuaFunction
    public Object[] getInfo() {
        return read(
                s ->
                        new Object[] {
                            s.doubleAt(POWER),
                            s.longAt(DELTA),
                            s.intAt(MODE_LOW),
                            s.intAt(MODE_HIGH),
                            s.intAt(PRIORITY)
                        });
    }
}
