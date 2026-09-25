// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.storage;

import com.hbm.api.energymk2.IEnergyHandlerMK2.ConnectionPriority;
import com.hbm.tileentity.machine.storage.BlockEntityBatteryREDD;
import dan200.computercraft.api.lua.LuaFunction;

public final class BatteryREDDPeripheral extends EnergyStoragePeripheral<BlockEntityBatteryREDD> {

    private static final int POWER = SLOTS;
    private static final int DELTA = SLOTS + 1;

    public BatteryREDDPeripheral(BlockEntityBatteryREDD machine) {
        super(machine, SLOTS + 2, 0);
    }

    @Override
    protected int redLow(BlockEntityBatteryREDD machine) {
        return machine.redLow;
    }

    @Override
    protected int redHigh(BlockEntityBatteryREDD machine) {
        return machine.redHigh;
    }

    @Override
    protected ConnectionPriority priority(BlockEntityBatteryREDD machine) {
        return machine.getPriority();
    }

    @Override
    protected void applyRedLow(BlockEntityBatteryREDD machine, int mode) {
        machine.redLow = mode;
    }

    @Override
    protected void applyRedHigh(BlockEntityBatteryREDD machine, int mode) {
        machine.redHigh = mode;
    }

    @Override
    protected void applyPriority(BlockEntityBatteryREDD machine, ConnectionPriority priority) {
        machine.priority = priority;
    }

    @Override
    protected void capture(BlockEntityBatteryREDD machine) {
        super.capture(machine);
        put(POWER, machine.bigPower.doubleValue());
        put(DELTA, machine.bigDelta.longValue());
    }

    @LuaFunction
    public final Object[] getEnergyInfo() {
        return read(s -> new Object[] {s.doubleAt(POWER), s.longAt(DELTA)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(
                s ->
                        new Object[] {
                            s.doubleAt(POWER),
                            s.longAt(DELTA),
                            s.intAt(RED_LOW),
                            s.intAt(RED_HIGH),
                            s.intAt(PRIORITY)
                        });
    }
}
