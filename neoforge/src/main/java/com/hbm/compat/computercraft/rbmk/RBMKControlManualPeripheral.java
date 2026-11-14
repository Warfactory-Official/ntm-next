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

import com.hbm.blockentity.machine.rbmk.RBMKManualControlBlockEntity;
import com.hbm.blockentity.machine.rbmk.RodGroup;
import com.hbm.compat.computercraft.OpenComputers;
import dan200.computercraft.api.lua.LuaFunction;

public final class RBMKControlManualPeripheral
        extends RBMKControlPeripheral<RBMKManualControlBlockEntity> {

    private static final int COLOR = CONTROL_SLOTS;

    public RBMKControlManualPeripheral(RBMKManualControlBlockEntity machine) {
        super(machine, CONTROL_SLOTS + 1);
    }

    @Override
    protected void capture(RBMKManualControlBlockEntity machine) {
        super.capture(machine);
        put(COLOR, machine.group == null ? -1 : machine.group.ordinal());
    }

    @LuaFunction
    public Object[] getColor() {
        return read(
                s ->
                        s.intAt(COLOR) < 0
                                ? OpenComputers.unknownError()
                                : new Object[] {s.intAt(COLOR)});
    }

    @LuaFunction(mainThread = true)
    public Object[] setColor(int color) {
        RBMKManualControlBlockEntity machine = machine();
        machine.setGroup(RodGroup.VALUES[Math.clamp(color, 0, 4)]);
        return new Object[] {true};
    }
}
