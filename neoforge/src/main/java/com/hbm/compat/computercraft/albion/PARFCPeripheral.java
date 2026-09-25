// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.albion;

import com.hbm.tileentity.machine.albion.BlockEntityPARFC;
import dan200.computercraft.api.lua.LuaFunction;

public final class PARFCPeripheral extends CooledPeripheral<BlockEntityPARFC> {

    public PARFCPeripheral(BlockEntityPARFC machine) {
        super(machine, "ntm_pa_rfc", SLOTS, 0);
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(
                s ->
                        new Object[] {
                            s.longAt(POWER),
                            s.longAt(MAX_POWER),
                            s.intAt(COOLANT),
                            s.intAt(COOLANT_MAX),
                            s.intAt(HOT),
                            s.intAt(HOT_MAX)
                        });
    }
}
