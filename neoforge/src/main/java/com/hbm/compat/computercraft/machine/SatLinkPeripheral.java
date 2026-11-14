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

import com.hbm.api.redstoneoverradio.RadioCommandParser;
import com.hbm.blockentity.machine.SatelliteLinkerBlockEntity;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import dan200.computercraft.api.lua.LuaFunction;

public final class SatLinkPeripheral extends SnapshotPeripheral<SatelliteLinkerBlockEntity> {

    private static final int CONNECTED = 0;
    private static final int FREQUENCY = 1;
    private static final int TYPE = 0;
    private static final int REPLY = 1;

    public SatLinkPeripheral(SatelliteLinkerBlockEntity machine) {
        super(machine, "ntm_satlink", 2, 2);
    }

    @Override
    protected void capture(SatelliteLinkerBlockEntity machine) {
        put(CONNECTED, machine.connected());
        put(FREQUENCY, machine.frequency());
        putRef(TYPE, machine.getRadioValue(RadioCommandParser.VAL + "type"));
        putRef(REPLY, machine.getRadioValue(RadioCommandParser.VAL + "rx"));
    }

    @LuaFunction
    public Object[] isConnected() {
        return new Object[] {booleanAt(CONNECTED)};
    }

    @LuaFunction(mainThread = true)
    public Object[] setFreq(int frequency) {
        machine().setFrequency(frequency);
        return new Object[] {};
    }

    @LuaFunction
    public Object[] getFreq() {
        return new Object[] {intAt(FREQUENCY)};
    }

    @LuaFunction("getType")
    public Object[] getSatelliteType() {
        return new Object[] {refAt(TYPE)};
    }

    @LuaFunction(mainThread = true)
    public Object[] send(String command) {
        machine().runRadioFunction(RadioCommandParser.FUN + "tx", new String[] {command});
        return new Object[] {};
    }

    @LuaFunction
    public Object[] read() {
        return new Object[] {refAt(REPLY)};
    }
}
