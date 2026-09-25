// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.machine;

import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.BlockEntityGeiger;
import dan200.computercraft.api.lua.LuaFunction;

public final class GeigerPeripheral extends SnapshotPeripheral<BlockEntityGeiger> {

    public GeigerPeripheral(BlockEntityGeiger machine) {
        super(machine, "ntm_geiger", 0, 0);
    }

    @Override
    protected void capture(BlockEntityGeiger machine) {}

    @LuaFunction(mainThread = true)
    public final Object[] getRads() {
        return new Object[] {machine().check()};
    }
}
