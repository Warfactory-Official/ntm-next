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

import com.hbm.blockentity.network.RadioTelexBlockEntity;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

public final class TelexPeripheral extends SnapshotPeripheral<RadioTelexBlockEntity> {

    private static final int LINES = 5;
    private static final int TX_CHANNEL = 0;
    private static final int RX_CHANNEL = 1;
    private static final int TX = 2;
    private static final int RX = TX + LINES;

    public TelexPeripheral(RadioTelexBlockEntity machine) {
        super(machine, "ntm_telex", 0, RX + LINES);
    }

    @Override
    protected void capture(RadioTelexBlockEntity machine) {
        putRef(TX_CHANNEL, machine.txChannel);
        putRef(RX_CHANNEL, machine.rxChannel);
        for (var i = 0; i < LINES; i++) {
            putRef(TX + i, machine.txBuffer[i]);
            putRef(RX + i, machine.rxBuffer[i]);
        }
    }

    @LuaFunction
    public Object[] getChannels() {
        return read(s -> new Object[] {s.refAt(TX_CHANNEL), s.refAt(RX_CHANNEL)});
    }

    @LuaFunction(mainThread = true)
    public Object[] setChannels(String receive, String transmit) {
        var machine = machine();
        var previous = new Object[] {machine.txChannel, machine.rxChannel};
        if (!transmit.equals(machine.txChannel) || !receive.equals(machine.rxChannel)) {
            machine.setChannels(transmit, receive);
        }
        return previous;
    }

    @LuaFunction
    public Object[] getSendingTexts() {
        return read(s -> lines(s, TX));
    }

    @LuaFunction
    public Object[] getReceivingText() {
        return read(s -> lines(s, RX));
    }

    private static Object[] lines(SnapshotPeripheral<?> s, int first) {
        var out = new Object[LINES];
        for (var i = 0; i < LINES; i++) out[i] = s.refAt(first + i);
        return out;
    }

    @LuaFunction(mainThread = true)
    public Object[] setSendingText(IArguments args) throws LuaException {
        if (args.count() < LINES) {
            throw new LuaException(
                    "bad arguments #" + (args.count() + 1) + " (value expected, got no value)");
        }
        var lines = new String[LINES];
        for (var i = 0; i < LINES; i++) {
            var line = args.optString(i, "");
            lines[i] =
                    line.length() > RadioTelexBlockEntity.LINE_WIDTH
                            ? line.substring(0, RadioTelexBlockEntity.LINE_WIDTH)
                            : line;
        }
        var machine = machine();
        var changed = false;
        for (var i = 0; i < LINES; i++) {
            if (lines[i].equals(machine.txBuffer[i])) continue;
            machine.setTransmitLine(i, lines[i]);
            changed = true;
        }
        if (changed) machine.setChanged();
        return new Object[] {true};
    }

    @LuaFunction(mainThread = true)
    public void printMessage() {
        machine().print();
    }

    @LuaFunction(mainThread = true)
    public void clearAll() {
        var machine = machine();
        var clear = machine.writingLine == 0;
        for (var i = 0; clear && i < LINES; i++) clear = machine.rxBuffer[i].isEmpty();
        if (!clear) machine.clearReceiveBuffer();
    }

    @LuaFunction(mainThread = true)
    public Object[] sendMessage() {
        var machine = machine();
        if (machine.isSending) return new Object[] {false};
        machine.isSending = true;
        machine.sendingLine = 0;
        machine.sendingIndex = 0;
        return new Object[] {true};
    }
}
