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

import com.hbm.blockentity.machine.albion.PADipoleBlockEntity;
import com.hbm.compat.computercraft.LuaEnums;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.util.Mth;

public final class AcceleratorDipolePeripheral extends CooledPeripheral<PADipoleBlockEntity> {

    private static final int LOWER = SLOTS;
    private static final int UPPER = SLOTS + 1;
    private static final int REDSTONE = SLOTS + 2;
    private static final int THRESHOLD = SLOTS + 3;
    private static final int MAX_THRESHOLD = 999_999_999;

    public AcceleratorDipolePeripheral(PADipoleBlockEntity machine) {
        super(machine, "ntm_pa_dipole", SLOTS + 4, 0);
    }

    @Override
    protected void capture(PADipoleBlockEntity machine) {
        super.capture(machine);
        put(LOWER, machine.dirLower);
        put(UPPER, machine.dirUpper);
        put(REDSTONE, machine.dirRedstone);
        put(THRESHOLD, machine.threshold);
    }

    @LuaFunction
    public Object[] getDirLower() {
        return new Object[] {LuaEnums.direction(intAt(LOWER))};
    }

    @LuaFunction
    public Object[] getDirUpper() {
        return new Object[] {LuaEnums.direction(intAt(UPPER))};
    }

    @LuaFunction
    public Object[] getDirRedstone() {
        return new Object[] {LuaEnums.direction(intAt(REDSTONE))};
    }

    @LuaFunction
    public Object[] getThreshold() {
        return new Object[] {intAt(THRESHOLD)};
    }

    @LuaFunction(mainThread = true)
    public void setDirLower(String name) {
        var code = LuaEnums.directionCode(name);
        var machine = machine();
        if (code < 0 || machine.dirLower == code) return;
        machine.hbm$syncSet$PADipoleBlockEntity$dirLower(code);
        machine.setChanged();
    }

    @LuaFunction(mainThread = true)
    public void setDirUpper(String name) {
        var code = LuaEnums.directionCode(name);
        var machine = machine();
        if (code < 0 || machine.dirUpper == code) return;
        machine.hbm$syncSet$PADipoleBlockEntity$dirUpper(code);
        machine.setChanged();
    }

    @LuaFunction(mainThread = true)
    public void setDirRedstone(String name) {
        var code = LuaEnums.directionCode(name);
        var machine = machine();
        if (code < 0 || machine.dirRedstone == code) return;
        machine.hbm$syncSet$PADipoleBlockEntity$dirRedstone(code);
        machine.setChanged();
    }

    @LuaFunction(mainThread = true)
    public void setThreshold(int value) {
        var threshold = Mth.clamp(value, 0, MAX_THRESHOLD);
        var machine = machine();
        if (machine.threshold != threshold) machine.setThreshold(threshold);
    }

    @LuaFunction
    public Object[] getInfo() {
        return read(
                s -> {
                    var out = new Object[10];
                    var at = CooledValues.energy(s, out, 0);
                    at = CooledValues.coolant(s, out, at);
                    out[at] = LuaEnums.direction(s.intAt(LOWER));
                    out[at + 1] = LuaEnums.direction(s.intAt(UPPER));
                    out[at + 2] = LuaEnums.direction(s.intAt(REDSTONE));
                    out[at + 3] = s.intAt(THRESHOLD);
                    return out;
                });
    }
}
