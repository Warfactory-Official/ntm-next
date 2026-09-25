// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.storage;

import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.storage.BlockEntityMachineBattery;
import dan200.computercraft.api.lua.LuaFunction;

public final class MachineBatteryPeripheral extends SnapshotPeripheral<BlockEntityMachineBattery> {

    private static final int POWER = 0;
    private static final int MAX_POWER = 1;

    public MachineBatteryPeripheral(BlockEntityMachineBattery machine) {
        super(machine, "ntm_energy_storage_legacy", 2, 0);
    }

    @Override
    protected void capture(BlockEntityMachineBattery machine) {
        put(POWER, machine.getPower());
        put(MAX_POWER, machine.getMaxPower());
    }

    @LuaFunction
    public final Object[] getEnergyInfo() {
        return read(s -> new Object[] {s.longAt(POWER), s.longAt(MAX_POWER)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(s -> new Object[] {s.longAt(POWER), s.longAt(MAX_POWER)});
    }
}
