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

import com.hbm.blockentity.network.FluidCounterValveBlockEntity;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

public final class CountingValvePeripheral
        extends SnapshotPeripheral<FluidCounterValveBlockEntity> {

    private static final int COUNTER = 0;
    private static final int OPEN = 1;
    private static final int FLUID = 0;

    public CountingValvePeripheral(FluidCounterValveBlockEntity machine) {
        super(machine, "ntm_fluid_counter_valve", 2, 1);
    }

    @Override
    protected void capture(FluidCounterValveBlockEntity machine) {
        put(COUNTER, machine.getTotal());
        put(OPEN, machine.isOpen());
        putRef(FLUID, PipeFluid.legacyName(machine));
    }

    @LuaFunction
    public Object[] getFluid() {
        return new Object[] {refAt(FLUID)};
    }

    @LuaFunction
    public Object[] getCounter() {
        return new Object[] {longAt(COUNTER)};
    }

    @LuaFunction(mainThread = true)
    public void resetCounter() {
        machine().resetCounter();
    }

    @LuaFunction
    public Object[] getState() {
        return new Object[] {booleanAt(OPEN) ? 1 : 0};
    }

    @LuaFunction(mainThread = true)
    public void setState(int state) throws LuaException {
        if (state != 0 && state != 1) throw new LuaException("bad argument");
        machine().setOpen(state == 1);
    }
}
