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

package com.hbm.compat.computercraft.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKBaseBlockEntity;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import dan200.computercraft.api.lua.LuaFunction;

public abstract class RBMKColumnPeripheral<BE extends RBMKBaseBlockEntity>
        extends SnapshotPeripheral<BE> {

    protected static final int HEAT = 0;
    protected static final int X = 1;
    protected static final int Y = 2;
    protected static final int Z = 3;
    protected static final int SLOTS = 4;

    protected RBMKColumnPeripheral(BE machine, String type, int longs, int refs) {
        super(machine, type, longs, refs);
    }

    @Override
    protected void capture(BE machine) {
        put(HEAT, machine.heat);
        put(X, machine.getBlockPos().getX());
        put(Y, machine.getBlockPos().getY());
        put(Z, machine.getBlockPos().getZ());
    }

    @LuaFunction
    public final Object[] getCoordinates() {
        return read(s -> new Object[] {s.intAt(X), s.intAt(Y), s.intAt(Z)});
    }
}
