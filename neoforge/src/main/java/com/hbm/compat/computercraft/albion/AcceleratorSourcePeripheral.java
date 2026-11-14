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

package com.hbm.compat.computercraft.albion;

import com.hbm.blockentity.machine.albion.PASourceBlockEntity.PAState;
import com.hbm.blockentity.machine.albion.PASourceBlockEntity;
import com.hbm.compat.computercraft.LuaEnums;
import dan200.computercraft.api.lua.LuaFunction;

public final class AcceleratorSourcePeripheral extends CooledPeripheral<PASourceBlockEntity> {

    private static final int MOMENTUM = SLOTS;
    private static final int COUNTS = SLOTS + 1;
    private static final int STATE = 0;
    private static final int NAMES = 1;

    public AcceleratorSourcePeripheral(PASourceBlockEntity machine) {
        super(machine, "ntm_pa_source", SLOTS + 5, 5);
    }

    @Override
    protected void capture(PASourceBlockEntity machine) {
        super.capture(machine);
        put(MOMENTUM, machine.lastSpeed);
        putRef(STATE, LuaEnums.acceleratorState(machine.state));
        captureCrafting(machine, COUNTS, NAMES);
    }

    @LuaFunction
    public Object[] getMomentum() {
        return new Object[] {intAt(MOMENTUM)};
    }

    @LuaFunction
    public Object[] getState() {
        return new Object[] {refAt(STATE)};
    }

    @LuaFunction
    public Object[] getCrafting() {
        return read(
                s -> {
                    var out = new Object[8];
                    CooledValues.crafting(s, out, 0, COUNTS, NAMES);
                    return out;
                });
    }

    @LuaFunction(mainThread = true)
    public void cancelOperation() {
        var machine = machine();
        if (machine.particle != null || machine.state != PAState.IDLE) machine.cancelParticle();
    }

    @LuaFunction
    public Object[] getInfo() {
        return read(
                s -> {
                    var out = new Object[16];
                    var at = CooledValues.energy(s, out, 0);
                    at = CooledValues.coolant(s, out, at);
                    at = CooledValues.crafting(s, out, at, COUNTS, NAMES);
                    out[at] = s.intAt(MOMENTUM);
                    out[at + 1] = s.refAt(STATE);
                    return out;
                });
    }
}
