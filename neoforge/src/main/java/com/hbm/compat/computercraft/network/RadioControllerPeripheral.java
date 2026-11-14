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

import com.hbm.blockentity.network.RadioTorchControllerBlockEntity;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import dan200.computercraft.api.lua.LuaFunction;

public final class RadioControllerPeripheral
        extends SnapshotPeripheral<RadioTorchControllerBlockEntity> {
    private static final int POLLING = 0;
    private static final int CHANNEL = 0;

    public RadioControllerPeripheral(RadioTorchControllerBlockEntity machine) {
        super(machine, "radio_controller", 1, 1);
    }

    @Override
    protected void capture(RadioTorchControllerBlockEntity machine) {
        put(POLLING, machine.polling);
        putRef(CHANNEL, machine.channel);
    }

    @LuaFunction(mainThread = true)
    public Object[] setChannel(String channel) {
        machine().setChannel(channel);
        return new Object[] {};
    }

    @LuaFunction
    public Object[] getChannel() {
        return new Object[] {refAt(CHANNEL)};
    }

    @LuaFunction(mainThread = true)
    public Object[] setPolling(boolean polling) {
        machine().setPolling(polling);
        return new Object[] {};
    }

    @LuaFunction
    public Object[] getPolling() {
        return new Object[] {booleanAt(POLLING)};
    }

    @LuaFunction(mainThread = true)
    public Object[] send(String command) {
        machine().send(command);
        return new Object[] {};
    }
}
