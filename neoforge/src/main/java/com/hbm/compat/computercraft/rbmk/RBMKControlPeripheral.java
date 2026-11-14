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

import com.hbm.blockentity.machine.rbmk.RBMKControlBlockEntity;
import dan200.computercraft.api.lua.LuaFunction;

public class RBMKControlPeripheral<BE extends RBMKControlBlockEntity>
        extends RBMKColumnPeripheral<BE> {

    protected static final int LEVEL = SLOTS;
    protected static final int TARGET = SLOTS + 1;
    protected static final int CONTROL_SLOTS = SLOTS + 2;

    public RBMKControlPeripheral(BE machine) {
        this(machine, CONTROL_SLOTS);
    }

    protected RBMKControlPeripheral(BE machine, int longs) {
        super(machine, "rbmk_control_rod", longs, 0);
    }

    @Override
    protected void capture(BE machine) {
        super.capture(machine);
        put(LEVEL, machine.fluxMultiplier());
        put(TARGET, machine.target);
    }

    @LuaFunction
    public final Object[] getLevel() {
        return read(s -> new Object[] {s.doubleAt(LEVEL) * 100});
    }

    @LuaFunction
    public final Object[] getTargetLevel() {
        return read(s -> new Object[] {s.doubleAt(TARGET) * 100});
    }

    @LuaFunction
    public final Object[] getHeat() {
        return new Object[] {doubleAt(HEAT)};
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(
                s ->
                        new Object[] {
                            s.doubleAt(HEAT),
                            s.doubleAt(LEVEL) * 100,
                            s.doubleAt(TARGET) * 100,
                            s.intAt(X),
                            s.intAt(Y),
                            s.intAt(Z)
                        });
    }

    @LuaFunction(mainThread = true)
    public final Object[] setLevel(double level) {
        BE machine = machine();
        machine.setTarget(level / 100.0);
        return new Object[] {};
    }
}
