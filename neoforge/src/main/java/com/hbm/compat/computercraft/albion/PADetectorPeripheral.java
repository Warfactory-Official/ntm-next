// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.albion;

import com.hbm.tileentity.machine.albion.BlockEntityPADetector;
import dan200.computercraft.api.lua.LuaFunction;

public final class PADetectorPeripheral extends CooledPeripheral<BlockEntityPADetector> {

    public PADetectorPeripheral(BlockEntityPADetector machine) {
        super(machine, "ntm_pa_detector", SLOTS + 4, 4);
    }

    @Override
    protected void capture(BlockEntityPADetector machine) {
        super.capture(machine);
        captureCrafting(machine, SLOTS, 0);
    }

    @LuaFunction
    public final Object[] getCrafting() {
        return read(s -> crafting(s, SLOTS, 0));
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(
                s -> {
                    Object[] items = crafting(s, SLOTS, 0);
                    return new Object[] {
                        s.longAt(POWER),
                        s.longAt(MAX_POWER),
                        s.intAt(COOLANT),
                        s.intAt(COOLANT_MAX),
                        s.intAt(HOT),
                        s.intAt(HOT_MAX),
                        items[0],
                        items[1],
                        items[2],
                        items[3],
                        items[4],
                        items[5],
                        items[6],
                        items[7]
                    };
                });
    }
}
