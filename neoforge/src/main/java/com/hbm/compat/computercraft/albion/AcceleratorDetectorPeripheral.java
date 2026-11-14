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

import com.hbm.blockentity.machine.albion.PADetectorBlockEntity;
import dan200.computercraft.api.lua.LuaFunction;

public final class AcceleratorDetectorPeripheral extends CooledPeripheral<PADetectorBlockEntity> {

    private static final int COUNTS = SLOTS;
    private static final int NAMES = 0;

    public AcceleratorDetectorPeripheral(PADetectorBlockEntity machine) {
        super(machine, "ntm_pa_detector", SLOTS + 4, 4);
    }

    @Override
    protected void capture(PADetectorBlockEntity machine) {
        super.capture(machine);
        captureCrafting(machine, COUNTS, NAMES);
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

    @LuaFunction
    public Object[] getInfo() {
        return read(
                s -> {
                    var out = new Object[14];
                    var at = CooledValues.energy(s, out, 0);
                    at = CooledValues.coolant(s, out, at);
                    CooledValues.crafting(s, out, at, COUNTS, NAMES);
                    return out;
                });
    }
}
