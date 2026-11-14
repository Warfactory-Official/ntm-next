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

import com.hbm.blockentity.network.RadioAutocalBlockEntity;
import com.hbm.compat.computercraft.BlockEntityPeripheral;
import dan200.computercraft.api.lua.LuaFunction;
import java.util.Arrays;

public final class AutocalControllerPeripheral
        extends BlockEntityPeripheral<RadioAutocalBlockEntity> {

    private static final Object[] NONE = new Object[0];

    public AutocalControllerPeripheral(RadioAutocalBlockEntity machine) {
        super(machine, "radio_autocal");
    }

    @LuaFunction(mainThread = true)
    public Object[] setBuffer(String text) {
        var machine = machine();
        var before = machine.executionContext().readBuffer();
        var complete = machine.executionContext().writeBuffer(text);
        if (!before.equals(machine.executionContext().readBuffer())) machine.setChanged();
        return new Object[] {complete};
    }

    @LuaFunction(mainThread = true)
    public Object[] getBuffer() {
        return new Object[] {machine().executionContext().readBuffer()};
    }

    @LuaFunction(mainThread = true)
    public void setScript(String text) {
        var machine = machine();
        if (!machine.running() && Arrays.equals(machine.programLines(), lines(text))) return;
        machine.replaceProgram(text);
    }

    private static String[] lines(String text) {
        var lines = text.split("\n");
        for (var i = 0; i < lines.length; i++) lines[i] = lines[i].trim();
        return lines;
    }

    @LuaFunction(mainThread = true)
    public Object[] getScript() {
        var script = machine().programLines();
        var joined = new StringBuilder();
        for (var line : script) joined.append(line).append('\n');
        return new Object[] {joined.toString()};
    }

    @LuaFunction(mainThread = true)
    public void setState(boolean on) {
        var machine = machine();
        if (on && machine.running()) return;
        machine.setRunning(on);
    }

    @LuaFunction
    public Object[] getState() {
        return new Object[] {target.running()};
    }

    @LuaFunction(mainThread = true)
    public void setIgnoreError(boolean ignore) {
        var machine = machine();
        if (machine.ignoresErrors() == ignore) return;
        machine.setErrorPolicy(ignore);
    }

    @LuaFunction
    public Object[] getIgnoreError() {
        return new Object[] {target.ignoresErrors()};
    }

    @LuaFunction(mainThread = true)
    public void setAutoReboot(boolean reboot) {
        var machine = machine();
        if (machine.rebootsAutomatically() == reboot) return;
        machine.setRebootPolicy(reboot);
    }

    @LuaFunction
    public Object[] getAutoReboot() {
        return new Object[] {target.rebootsAutomatically()};
    }

    @LuaFunction(mainThread = true)
    public Object[] getHistory(int index) {
        var machine = machine();
        return index >= 0 && index < machine.historySize()
                ? new Object[] {machine.historyLine(index).getString()}
                : NONE;
    }
}
