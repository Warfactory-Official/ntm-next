// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.storage;

import com.hbm.api.energymk2.IEnergyHandlerMK2.ConnectionPriority;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.storage.BlockEntityMachineBattery;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.level.block.entity.BlockEntity;

public abstract class EnergyStoragePeripheral<BE extends BlockEntity>
        extends SnapshotPeripheral<BE> {

    protected static final int RED_LOW = 0;
    protected static final int RED_HIGH = 1;
    protected static final int PRIORITY = 2;
    protected static final int SLOTS = 3;

    protected EnergyStoragePeripheral(BE machine, int longs, int refs) {
        super(machine, "ntm_energy_storage", longs, refs);
    }

    protected abstract int redLow(BE machine);

    protected abstract int redHigh(BE machine);

    protected abstract ConnectionPriority priority(BE machine);

    protected abstract void applyRedLow(BE machine, int mode);

    protected abstract void applyRedHigh(BE machine, int mode);

    protected abstract void applyPriority(BE machine, ConnectionPriority priority);

    @Override
    protected void capture(BE machine) {
        put(RED_LOW, redLow(machine));
        put(RED_HIGH, redHigh(machine));
        put(PRIORITY, priority(machine).ordinal() - 1);
    }

    @LuaFunction
    public final Object[] getModeInfo() {
        return read(s -> new Object[] {s.intAt(RED_LOW), s.intAt(RED_HIGH), s.intAt(PRIORITY)});
    }

    @LuaFunction(mainThread = true)
    public final Object[] setModeLow(int mode) {
        short newMode = (short) mode;
        if (newMode < BlockEntityMachineBattery.MODE_INPUT
                || newMode > BlockEntityMachineBattery.MODE_NONE) {
            return new Object[] {"Invalid mode"};
        }
        BE machine = machine();
        applyRedLow(machine, newMode);
        machine.setChanged();
        return new Object[] {};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setModeHigh(int mode) {
        short newMode = (short) mode;
        if (newMode < BlockEntityMachineBattery.MODE_INPUT
                || newMode > BlockEntityMachineBattery.MODE_NONE) {
            return new Object[] {"Invalid mode"};
        }
        BE machine = machine();
        applyRedHigh(machine, newMode);
        machine.setChanged();
        return new Object[] {};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setPriority(int priority) {
        if (priority < 0 || priority > 2) return new Object[] {"Invalid mode"};
        BE machine = machine();
        applyPriority(machine, ConnectionPriority.values()[priority + 1]);
        machine.setChanged();
        return new Object[] {};
    }
}
