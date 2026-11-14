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

import com.hbm.blockentity.machine.dfc.DfcEmitterBlockEntity;
import com.hbm.compat.computercraft.BlockEntityPeripheral;
import dan200.computercraft.api.lua.LuaFunction;

public final class CoreEmitterPeripheral extends BlockEntityPeripheral<DfcEmitterBlockEntity> {

    public CoreEmitterPeripheral(DfcEmitterBlockEntity machine) {
        super(machine, "dfc_emitter");
    }

    @LuaFunction
    public Object[] getEnergyInfo() {
        return new Object[] {(long) target.getPower(), (long) target.getMaxPower()};
    }

    @LuaFunction
    public Object[] getCryogel() {
        return new Object[] {(int) target.tank.getFill()};
    }

    @LuaFunction
    public Object[] getInput() {
        return new Object[] {(int) target.watts};
    }

    @LuaFunction
    public Object[] getInfo() {
        return new Object[] {
            (long) target.getPower(),
            (long) target.getMaxPower(),
            (int) target.tank.getFill(),
            (int) target.watts,
            target.isOn
        };
    }

    @LuaFunction
    public Object[] isActive() {
        return new Object[] {target.isOn};
    }

    @LuaFunction(mainThread = true)
    public Object[] setActive(boolean active) {
        DfcEmitterBlockEntity machine = machine();
        machine.setActive(active);
        return new Object[] {};
    }

    @LuaFunction(mainThread = true)
    public Object[] setInput(int input) {
        DfcEmitterBlockEntity machine = machine();
        machine.setInputPercent(input);
        return new Object[] {};
    }
}
