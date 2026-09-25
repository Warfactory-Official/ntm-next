// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.machine;

import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.tileentity.machine.BlockEntityCoreInjector;
import dan200.computercraft.api.lua.LuaFunction;

public final class CoreInjectorPeripheral extends SnapshotPeripheral<BlockEntityCoreInjector> {

    private static final int FILL_0 = 0;
    private static final int FILL_1 = 1;
    private static final int TYPE_0 = 0;
    private static final int TYPE_1 = 1;

    public CoreInjectorPeripheral(BlockEntityCoreInjector machine) {
        super(machine, "dfc_injector", 2, 2);
    }

    @Override
    protected void capture(BlockEntityCoreInjector machine) {
        put(FILL_0, machine.tanks[0].getFill());
        put(FILL_1, machine.tanks[1].getFill());
        putRef(TYPE_0, NTMFluids.legacyName(machine.tanks[0].getTankType()));
        putRef(TYPE_1, NTMFluids.legacyName(machine.tanks[1].getTankType()));
    }

    @LuaFunction
    public final Object[] getFuel() {
        return read(s -> new Object[] {s.intAt(FILL_0), s.intAt(FILL_1)});
    }

    @LuaFunction
    public final Object[] getTypes() {
        return read(s -> new Object[] {s.refAt(TYPE_0), s.refAt(TYPE_1)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(
                s ->
                        new Object[] {
                            s.intAt(FILL_0), s.refAt(TYPE_0), s.intAt(FILL_1), s.refAt(TYPE_1)
                        });
    }
}
