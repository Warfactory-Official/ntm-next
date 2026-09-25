// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.machine;

import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.BlockEntityWatz;
import dan200.computercraft.api.lua.LuaFunction;

public final class WatzPeripheral extends SnapshotPeripheral<BlockEntityWatz> {

    private static final int FILL = 0;
    private static final int HEAT = 6;
    private static final int FLUX = 7;
    private static final int ON = 8;

    public WatzPeripheral(BlockEntityWatz machine) {
        super(machine, "watz_reactor", 9, 0);
    }

    @Override
    protected void capture(BlockEntityWatz machine) {
        for (int i = 0; i < 3; i++) {
            put(FILL + 2 * i, machine.tanks[i].getFill());
            put(FILL + 2 * i + 1, machine.tanks[i].getMaxFill());
        }
        put(HEAT, machine.heat);
        put(FLUX, machine.fluxLastBase + machine.fluxLastReaction);
        put(ON, machine.isOn);
    }

    @LuaFunction
    public final Object[] getHeat() {
        return read(s -> new Object[] {s.intAt(HEAT)});
    }

    @LuaFunction
    public final Object[] getFlux() {
        return read(s -> new Object[] {s.doubleAt(FLUX)});
    }

    @LuaFunction
    public final Object[] getCoolantInfo() {
        return read(s -> new Object[] {s.intAt(0), s.intAt(1), s.intAt(2), s.intAt(3)});
    }

    @LuaFunction
    public final Object[] getWasteInfo() {
        return read(s -> new Object[] {s.intAt(4), s.intAt(5)});
    }

    @LuaFunction
    public final Object[] isOn() {
        return read(s -> new Object[] {s.booleanAt(ON)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(
                s ->
                        new Object[] {
                            s.intAt(0),
                            s.intAt(1),
                            s.intAt(2),
                            s.intAt(3),
                            s.intAt(4),
                            s.intAt(5),
                            s.intAt(HEAT),
                            s.doubleAt(FLUX),
                            s.booleanAt(ON)
                        });
    }
}
