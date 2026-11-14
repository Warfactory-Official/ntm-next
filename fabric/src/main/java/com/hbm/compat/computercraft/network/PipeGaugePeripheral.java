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

import com.hbm.blockentity.network.PipeGaugeBlockEntity;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import dan200.computercraft.api.lua.LuaFunction;

public final class PipeGaugePeripheral extends SnapshotPeripheral<PipeGaugeBlockEntity> {

    private static final int DELTA_TICK = 0;
    private static final int DELTA_SECOND = 1;
    private static final int X = 2;
    private static final int Y = 3;
    private static final int Z = 4;
    private static final int FLUID = 0;

    public PipeGaugePeripheral(PipeGaugeBlockEntity machine) {
        super(machine, "ntm_fluid_gauge", 5, 1);
    }

    @Override
    protected void capture(PipeGaugeBlockEntity machine) {
        put(DELTA_TICK, machine.getTransferredThisTick());
        put(DELTA_SECOND, machine.getTransferredLastInterval());
        put(X, machine.getBlockPos().getX());
        put(Y, machine.getBlockPos().getY());
        put(Z, machine.getBlockPos().getZ());
        putRef(FLUID, PipeFluid.legacyName(machine));
    }

    @LuaFunction
    public Object[] getTransfer() {
        return read(s -> new Object[] {s.longAt(DELTA_TICK), s.longAt(DELTA_SECOND)});
    }

    @LuaFunction
    public Object[] getFluid() {
        return new Object[] {refAt(FLUID)};
    }

    @LuaFunction
    public Object[] getInfo() {
        return read(
                s ->
                        new Object[] {
                            s.longAt(DELTA_TICK),
                            s.longAt(DELTA_SECOND),
                            s.refAt(FLUID),
                            s.intAt(X),
                            s.intAt(Y),
                            s.intAt(Z)
                        });
    }
}
