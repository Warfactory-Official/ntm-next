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

import com.hbm.blockentity.machine.rbmk.RBMKCoolerBlockEntity;
import dan200.computercraft.api.lua.LuaFunction;

public final class RBMKCoolerPeripheral extends RBMKColumnPeripheral<RBMKCoolerBlockEntity> {

    private static final int COLD = SLOTS;
    private static final int COLD_MAX = SLOTS + 1;
    private static final int HOT = SLOTS + 2;
    private static final int HOT_MAX = SLOTS + 3;

    public RBMKCoolerPeripheral(RBMKCoolerBlockEntity machine) {
        super(machine, "rbmk_cooler", SLOTS + 4, 0);
    }

    @Override
    protected void capture(RBMKCoolerBlockEntity machine) {
        super.capture(machine);
        put(COLD, machine.coolant.getFill());
        put(COLD_MAX, machine.coolant.getMaxFill());
        put(HOT, machine.product.getFill());
        put(HOT_MAX, machine.product.getMaxFill());
    }

    @LuaFunction
    public Object[] getHeat() {
        return new Object[] {doubleAt(HEAT)};
    }

    @LuaFunction
    public Object[] getCoolant() {
        return read(
                s ->
                        new Object[] {
                            s.intAt(COLD), s.intAt(COLD_MAX), s.intAt(HOT), s.intAt(HOT_MAX)
                        });
    }

    @LuaFunction
    public Object[] getInfo() {
        return read(
                s ->
                        new Object[] {
                            s.doubleAt(HEAT),
                            s.intAt(COLD),
                            s.intAt(COLD_MAX),
                            s.intAt(HOT),
                            s.intAt(HOT_MAX),
                            s.intAt(X),
                            s.intAt(Y),
                            s.intAt(Z)
                        });
    }
}
