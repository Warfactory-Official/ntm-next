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

import com.hbm.blockentity.network.RadioReaderBlockEntity;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

public final class RadioValueReaderPeripheral extends SnapshotPeripheral<RadioReaderBlockEntity> {

    private static final int SLOTS = 8;
    private static final int POLLING = 0;
    private static final int CHANNELS = 0;
    private static final int NAMES = SLOTS;
    private static final int SENT = 2 * SLOTS;
    private static final Object[] NONE = new Object[0];

    public RadioValueReaderPeripheral(RadioReaderBlockEntity machine) {
        super(machine, "radio_reader", 1, 3 * SLOTS);
    }

    @Override
    protected void capture(RadioReaderBlockEntity machine) {
        put(POLLING, machine.polling);
        for (var i = 0; i < SLOTS; i++) {
            putRef(CHANNELS + i, machine.channels[i]);
            putRef(NAMES + i, machine.names[i]);
            putRef(SENT + i, machine.previous[i]);
        }
    }

    private static boolean slot(int index) {
        return index >= 0 && index < SLOTS;
    }

    private Object[] slotRef(int index, int base) {
        return slot(index) ? new Object[] {refAt(base + index)} : NONE;
    }

    @LuaFunction(mainThread = true)
    public void setChannel(IArguments args) throws LuaException {
        var index = args.getInt(0);
        if (slot(index)) machine().setChannel(index, args.getString(1));
    }

    @LuaFunction
    public Object[] getChannel(int index) {
        return slotRef(index, CHANNELS);
    }

    @LuaFunction(mainThread = true)
    public void setName(IArguments args) throws LuaException {
        var index = args.getInt(0);
        if (slot(index)) machine().setName(index, args.getString(1));
    }

    @LuaFunction
    public Object[] getName(int index) {
        return slotRef(index, NAMES);
    }

    @LuaFunction(mainThread = true)
    public void setPolling(boolean polling) {
        var machine = machine();
        if (machine.polling != polling) machine.setPolling(polling);
    }

    @LuaFunction
    public Object[] getPolling() {
        return new Object[] {booleanAt(POLLING)};
    }

    @LuaFunction
    public Object[] read(int index) {
        return slotRef(index, SENT);
    }
}
