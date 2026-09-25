// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.rbmk;

import com.hbm.compat.computercraft.OpenComputers;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControlManual;
import com.hbm.tileentity.machine.rbmk.RBMKColor;
import dan200.computercraft.api.lua.LuaFunction;

public final class RBMKControlManualPeripheral
        extends RBMKControlPeripheral<BlockEntityRBMKControlManual> {

    private static final int COLOR = CONTROL_SLOTS;

    public RBMKControlManualPeripheral(BlockEntityRBMKControlManual machine) {
        super(machine, CONTROL_SLOTS + 1);
    }

    @Override
    protected void capture(BlockEntityRBMKControlManual machine) {
        super.capture(machine);
        put(COLOR, machine.color == null ? -1 : machine.color.ordinal());
    }

    @LuaFunction
    public final Object[] getColor() {

        return read(
                s ->
                        s.intAt(COLOR) < 0
                                ? OpenComputers.unknownError()
                                : new Object[] {s.intAt(COLOR)});
    }

    @LuaFunction(mainThread = true)
    public final Object[] setColor(int color) {
        BlockEntityRBMKControlManual machine = machine();
        machine.color = RBMKColor.VALUES[Math.clamp(color, 0, 4)];
        machine.setChanged();
        return new Object[] {true};
    }
}
