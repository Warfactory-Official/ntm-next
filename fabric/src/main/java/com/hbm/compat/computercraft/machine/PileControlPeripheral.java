// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.machine;

import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.pile.BlockEntityPileControl;
import dan200.computercraft.api.lua.LuaFunction;

public final class PileControlPeripheral extends SnapshotPeripheral<BlockEntityPileControl> {
    private static final int LEVEL = 0;

    public PileControlPeripheral(BlockEntityPileControl machine) {
        super(machine, "ntm_pile_control", 1, 0);
    }

    @Override
    protected void capture(BlockEntityPileControl machine) {
        put(LEVEL, machine.extension);
    }

    @LuaFunction
    public final Object[] getLevel() {
        return read(s -> new Object[] {s.doubleAt(LEVEL)});
    }

    @LuaFunction(mainThread = true)
    public final Object[] setLevel(double percent) {
        machine().setTarget(Math.clamp(percent / 100D, 0D, 1D));
        return new Object[] {};
    }
}
