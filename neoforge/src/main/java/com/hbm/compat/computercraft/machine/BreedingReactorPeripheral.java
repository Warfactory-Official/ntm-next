// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.machine;

import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.BlockEntityMachineReactorBreeding;
import dan200.computercraft.api.lua.LuaFunction;

public final class BreedingReactorPeripheral
        extends SnapshotPeripheral<BlockEntityMachineReactorBreeding> {

    private static final int FLUX = 0;
    private static final int PROGRESS = 1;

    public BreedingReactorPeripheral(BlockEntityMachineReactorBreeding machine) {
        super(machine, "breeding_reactor", 2, 0);
    }

    @Override
    protected void capture(BlockEntityMachineReactorBreeding machine) {
        put(FLUX, machine.flux);
        put(PROGRESS, machine.progress);
    }

    @LuaFunction
    public final Object[] getFlux() {
        return read(s -> new Object[] {s.intAt(FLUX)});
    }

    @LuaFunction
    public final Object[] getProgress() {
        return read(s -> new Object[] {(float) s.doubleAt(PROGRESS)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(s -> new Object[] {s.intAt(FLUX), (float) s.doubleAt(PROGRESS)});
    }
}
