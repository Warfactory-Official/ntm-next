// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.machine;

import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.BlockEntityCoreReceiver;
import dan200.computercraft.api.lua.LuaFunction;

public final class CoreReceiverPeripheral extends SnapshotPeripheral<BlockEntityCoreReceiver> {

    private static final int JOULES = 0;
    private static final int POWER = 1;
    private static final int CRYOGEL = 2;

    public CoreReceiverPeripheral(BlockEntityCoreReceiver machine) {
        super(machine, "dfc_receiver", 3, 0);
    }

    @Override
    protected void capture(BlockEntityCoreReceiver machine) {
        put(JOULES, machine.joules);
        put(POWER, machine.getPower());
        put(CRYOGEL, machine.tank.getFill());
    }

    @LuaFunction
    public final Object[] getEnergyInfo() {
        return read(s -> new Object[] {s.longAt(JOULES), s.longAt(POWER)});
    }

    @LuaFunction
    public final Object[] getCryogel() {
        return read(s -> new Object[] {s.intAt(CRYOGEL)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(s -> new Object[] {s.longAt(JOULES), s.longAt(POWER), s.intAt(CRYOGEL)});
    }
}
