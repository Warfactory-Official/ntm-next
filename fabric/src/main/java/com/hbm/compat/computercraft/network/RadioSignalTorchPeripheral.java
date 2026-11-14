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

import com.hbm.blockentity.network.RadioTorchBlockEntity;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import dan200.computercraft.api.lua.LuaFunction;
import java.util.Map;

public final class RadioSignalTorchPeripheral extends SnapshotPeripheral<RadioTorchBlockEntity> {

    private static final int POLLING = 0;
    private static final int CUSTOM = 1;
    private static final int CHANNEL = 0;
    private static final int MAPPING = 1;
    private static final int STRENGTHS = 16;

    public RadioSignalTorchPeripheral(RadioTorchBlockEntity machine) {
        super(machine, "radio_torch", 2, MAPPING + STRENGTHS);
    }

    @Override
    protected void capture(RadioTorchBlockEntity machine) {
        put(POLLING, machine.polling);
        put(CUSTOM, machine.customMap);
        putRef(CHANNEL, machine.channel);
        for (var i = 0; i < STRENGTHS; i++) putRef(MAPPING + i, machine.mapping[i]);
    }

    @LuaFunction
    public Object[] getChannel() {
        return new Object[] {refAt(CHANNEL)};
    }

    @LuaFunction
    public Object[] getPolling() {
        return new Object[] {booleanAt(POLLING)};
    }

    @LuaFunction
    public Object[] getCustomMap() {
        return new Object[] {booleanAt(CUSTOM)};
    }

    @LuaFunction
    public Object[] getCustomMapValues() {
        return read(
                s -> {
                    var mapping = new String[STRENGTHS];
                    for (var i = 0; i < STRENGTHS; i++) mapping[i] = (String) s.refAt(MAPPING + i);
                    return new Object[] {mapping};
                });
    }

    @LuaFunction(mainThread = true)
    public void setChannel(String channel) {
        var machine = machine();
        if (!channel.equals(machine.channel)) machine.setMappedChannel(channel);
    }

    @LuaFunction(mainThread = true)
    public void setPolling(boolean polling) {
        var machine = machine();
        if (machine.polling != polling) machine.setPolling(polling);
    }

    @LuaFunction(mainThread = true)
    public void setCustomMap(boolean custom) {
        var machine = machine();
        if (machine.customMap != custom) machine.setCustomMap(custom);
    }

    @LuaFunction(mainThread = true)
    public void setCustomMapValues(Map<?, ?> values) {
        var machine = machine();
        var changed = false;
        for (var entry : values.entrySet()) {
            if (!(entry.getKey() instanceof Number key)
                    || !(entry.getValue() instanceof String text)) continue;
            var k = key.doubleValue();
            if (k < 1 || k > STRENGTHS || k != Math.rint(k)) continue;
            var index = (int) k - 1;
            if (text.equals(machine.mapping[index])) continue;
            machine.setMapping(index, text);
            changed = true;
        }
        if (changed) machine.setChanged();
    }
}
