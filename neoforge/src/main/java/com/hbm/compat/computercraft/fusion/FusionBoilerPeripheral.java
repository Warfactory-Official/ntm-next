// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.fusion;

import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionBoiler;
import dan200.computercraft.api.lua.LuaFunction;

public final class FusionBoilerPeripheral extends SnapshotPeripheral<BlockEntityFusionBoiler> {

    private static final int PLASMA = 0;
    private static final int FILL_0 = 1;
    private static final int MAX_0 = 2;
    private static final int FILL_1 = 3;
    private static final int MAX_1 = 4;

    public FusionBoilerPeripheral(BlockEntityFusionBoiler machine) {
        super(machine, "ntm_fusion_boiler", 5, 0);
    }

    @Override
    protected void capture(BlockEntityFusionBoiler machine) {
        put(PLASMA, machine.plasmaEnergySync);
        put(FILL_0, machine.tanks[0].getFill());
        put(MAX_0, machine.tanks[0].getMaxFill());
        put(FILL_1, machine.tanks[1].getFill());
        put(MAX_1, machine.tanks[1].getMaxFill());
    }

    @LuaFunction
    public final Object[] getPlasmaEnergy() {
        return read(s -> new Object[] {s.longAt(PLASMA)});
    }

    @LuaFunction
    public final Object[] getFluid() {
        return read(
                s ->
                        new Object[] {
                            s.intAt(FILL_0), s.intAt(MAX_0), s.intAt(FILL_1), s.intAt(MAX_1)
                        });
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(
                s ->
                        new Object[] {
                            s.longAt(PLASMA),
                            s.intAt(FILL_0),
                            s.intAt(MAX_0),
                            s.intAt(FILL_1),
                            s.intAt(MAX_1)
                        });
    }
}
