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

import com.hbm.blockentity.machine.storage.EnergyTransferSettings;
import com.hbm.blockentity.machine.storage.TransferMode;
import com.hbm.compat.computercraft.LuaEnums;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.level.block.entity.BlockEntity;

public abstract class StorageModesPeripheral<BE extends BlockEntity & EnergyTransferSettings>
        extends SnapshotPeripheral<BE> {

    protected static final int MODE_LOW = 0;
    protected static final int MODE_HIGH = 1;
    protected static final int PRIORITY = 2;
    protected static final int MODE_NUMBERS = 3;

    private static final int MODE_INPUT = 0;
    private static final int MODE_NONE = 3;
    private static final String INVALID = "Invalid mode";

    protected StorageModesPeripheral(BE machine, int numbers, int texts) {
        super(machine, "ntm_energy_storage", numbers, texts);
    }

    @Override
    protected void capture(BE machine) {
        put(MODE_LOW, machine.getLowMode().code());
        put(MODE_HIGH, machine.getHighMode().code());
        put(PRIORITY, LuaEnums.storagePriority(machine.getEnergyTier()));
    }

    @LuaFunction
    public final Object[] getModeInfo() {
        return read(s -> new Object[] {s.intAt(MODE_LOW), s.intAt(MODE_HIGH), s.intAt(PRIORITY)});
    }

    private static boolean validMode(int mode) {
        var narrowed = (short) mode;
        return narrowed >= MODE_INPUT && narrowed <= MODE_NONE;
    }

    @LuaFunction(mainThread = true)
    public final Object[] setModeLow(int mode) {
        if (!validMode(mode)) return new Object[] {INVALID};
        machine().setLowMode(TransferMode.of((short) mode));
        return new Object[0];
    }

    @LuaFunction(mainThread = true)
    public final Object[] setModeHigh(int mode) {
        if (!validMode(mode)) return new Object[] {INVALID};
        machine().setHighMode(TransferMode.of((short) mode));
        return new Object[0];
    }

    @LuaFunction(mainThread = true)
    public final Object[] setPriority(int index) {
        var priority = LuaEnums.storagePriority(index);
        if (priority == null) return new Object[] {INVALID};
        machine().setPriority(priority);
        return new Object[0];
    }
}
