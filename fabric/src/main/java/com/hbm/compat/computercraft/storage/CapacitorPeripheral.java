// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.storage;

import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.storage.BlockEntityMachineCapacitor;
import dan200.computercraft.api.lua.LuaFunction;

public final class CapacitorPeripheral extends SnapshotPeripheral<BlockEntityMachineCapacitor> {

    private static final int POWER = 0;
    private static final int MAX_POWER = 1;
    private static final int RECEIVED = 2;
    private static final int SENT = 3;

    public CapacitorPeripheral(BlockEntityMachineCapacitor machine) {
        super(machine, "capacitor", 4, 0);
    }

    @Override
    protected void capture(BlockEntityMachineCapacitor machine) {
        put(POWER, machine.getPower());
        put(MAX_POWER, machine.getMaxPower());
        put(RECEIVED, machine.lastPowerReceived());
        put(SENT, machine.lastPowerSent());
    }

    @LuaFunction
    public final Object[] getEnergy() {
        return read(s -> new Object[] {s.longAt(POWER)});
    }

    @LuaFunction
    public final Object[] getMaxEnergy() {
        return read(s -> new Object[] {s.longAt(MAX_POWER)});
    }

    @LuaFunction
    public final Object[] getEnergySent() {
        return read(s -> new Object[] {s.longAt(RECEIVED)});
    }

    @LuaFunction
    public final Object[] getEnergyReceived() {
        return read(s -> new Object[] {s.longAt(SENT)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(
                s ->
                        new Object[] {
                            s.longAt(POWER), s.longAt(MAX_POWER), s.longAt(RECEIVED), s.longAt(SENT)
                        });
    }
}
