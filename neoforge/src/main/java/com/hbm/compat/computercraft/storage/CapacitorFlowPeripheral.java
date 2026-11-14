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

package com.hbm.compat.computercraft.storage;

import com.hbm.blockentity.machine.storage.CapacitorBlockEntity;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import dan200.computercraft.api.lua.LuaFunction;

public final class CapacitorFlowPeripheral extends SnapshotPeripheral<CapacitorBlockEntity> {

    private static final int STORED = 0;
    private static final int CAPACITY = 1;
    private static final int RECEIVED = 2;
    private static final int SENT = 3;

    public CapacitorFlowPeripheral(CapacitorBlockEntity machine) {
        super(machine, "capacitor", 4, 0);
    }

    @Override
    protected void capture(CapacitorBlockEntity machine) {
        put(STORED, machine.getPower());
        put(CAPACITY, machine.getMaxPower());
        put(RECEIVED, machine.lastPowerReceived());
        put(SENT, machine.lastPowerSent());
    }

    @LuaFunction
    public Object[] getEnergy() {
        return new Object[] {longAt(STORED)};
    }

    @LuaFunction
    public Object[] getMaxEnergy() {
        return new Object[] {longAt(CAPACITY)};
    }

    @LuaFunction
    public Object[] getEnergySent() {
        return new Object[] {longAt(SENT)};
    }

    @LuaFunction
    public Object[] getEnergyReceived() {
        return new Object[] {longAt(RECEIVED)};
    }

    @LuaFunction
    public Object[] getInfo() {
        return read(
                s ->
                        new Object[] {
                            s.longAt(STORED), s.longAt(CAPACITY), s.longAt(RECEIVED), s.longAt(SENT)
                        });
    }
}
