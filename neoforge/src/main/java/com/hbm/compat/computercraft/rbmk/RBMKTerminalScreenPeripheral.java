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

import com.hbm.blockentity.machine.rbmk.RBMKTerminalBlockEntity;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

public final class RBMKTerminalScreenPeripheral
        extends SnapshotPeripheral<RBMKTerminalBlockEntity> {

    private static final int OC_MODE = 0;
    private static final int LINES = RBMKTerminalBlockEntity.HISTORY;
    private static final String OFF = "OC mode not enabled";

    public RBMKTerminalScreenPeripheral(RBMKTerminalBlockEntity machine) {
        super(machine, "rbmk_terminal", 1, LINES);
    }

    @Override
    protected void capture(RBMKTerminalBlockEntity machine) {
        put(OC_MODE, machine.ocMode);
        for (var i = 0; i < LINES; i++) putRef(i, machine.history[i]);
    }

    private static boolean clear(RBMKTerminalBlockEntity machine) {
        var cleared = false;
        for (var i = 0; i < LINES; i++) {
            if ("".equals(machine.history[i])) continue;
            machine.hbm$syncArraySet$RBMKTerminalBlockEntity$history(machine.history, i, "");
            cleared = true;
        }
        return cleared;
    }

    @LuaFunction(mainThread = true)
    public Object[] enableOCMode(boolean on) {
        var machine = machine();
        var changed = machine.ocMode != on;
        machine.ocMode = on;
        if (on) {
            clear(machine);
            machine.push("OC MODE ENABLED");
            machine.push("Terminal ready.");
        } else if (changed) {
            machine.markChanged();
        }
        return new Object[] {true};
    }

    @LuaFunction
    public Object[] isOCMode() {
        return new Object[] {booleanAt(OC_MODE)};
    }

    @LuaFunction(mainThread = true)
    public Object[] write(IArguments args) throws LuaException {
        return push(args);
    }

    @LuaFunction(mainThread = true)
    public Object[] writeln(IArguments args) throws LuaException {
        return push(args);
    }

    private Object[] push(IArguments args) throws LuaException {
        var machine = machine();
        if (!machine.ocMode) return new Object[] {false, OFF};
        machine.push(args.getString(0));
        return new Object[] {true};
    }

    @LuaFunction
    public Object[] readInput() {
        return read(
                s -> {
                    if (!s.booleanAt(OC_MODE)) return new Object[] {"", OFF};
                    var newest = s.refAt(0);
                    return new Object[] {newest == null ? "" : newest};
                });
    }

    @LuaFunction
    public Object[] getAllHistory() {
        return read(
                s -> {
                    if (!s.booleanAt(OC_MODE)) return new Object[] {null, OFF};
                    var lines = new Object[LINES];
                    for (var i = 0; i < LINES; i++) {
                        var line = s.refAt(i);
                        lines[i] = line == null ? "" : line;
                    }
                    return new Object[] {lines};
                });
    }

    @LuaFunction(mainThread = true)
    public Object[] clearScreen() {
        var machine = machine();
        if (!machine.ocMode) return new Object[] {false, OFF};
        if (clear(machine)) machine.markChanged();
        return new Object[] {true};
    }
}
