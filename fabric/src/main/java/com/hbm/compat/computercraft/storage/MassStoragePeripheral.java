// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.storage;

import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.storage.BlockEntityMassStorage;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.item.ItemStack;

public final class MassStoragePeripheral extends SnapshotPeripheral<BlockEntityMassStorage> {

    private static final int FILL = 0;
    private static final int CAPACITY = 1;
    private static final int OUTPUT = 2;

    public MassStoragePeripheral(BlockEntityMassStorage machine) {
        super(machine, "ntm_mass_storage", 3, 0);
    }

    @Override
    protected void capture(BlockEntityMassStorage machine) {
        put(FILL, machine.getStockpile());
        put(CAPACITY, machine.getCapacity());
        put(OUTPUT, machine.output);
    }

    @LuaFunction
    public final Object[] getFill() {
        return read(s -> new Object[] {s.intAt(FILL)});
    }

    @LuaFunction
    public final Object[] getCapacity() {
        return read(s -> new Object[] {s.intAt(CAPACITY)});
    }

    @LuaFunction(value = "getType", mainThread = true)
    public final Object[] storedType() {
        ItemStack type = machine().getItem(BlockEntityMassStorage.SLOT_TYPE);
        return new Object[] {type.isEmpty() ? "None" : type.getHoverName().getString()};
    }

    @LuaFunction
    public final Object[] getOutputMode() {
        return read(s -> new Object[] {s.booleanAt(OUTPUT)});
    }

    @LuaFunction(mainThread = true)
    public final Object[] setOutputMode(boolean mode) {
        BlockEntityMassStorage machine = machine();
        machine.output = mode;
        machine.setChanged();
        return new Object[] {};
    }
}
