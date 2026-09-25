// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.storage;

import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.level.block.entity.BlockEntity;

public abstract class FluidTankPeripheral<BE extends BlockEntity> extends SnapshotPeripheral<BE> {

    private static final int FILL = 0;
    private static final int MAX = 1;
    private static final int TYPE = 0;

    protected FluidTankPeripheral(BE machine) {
        super(machine, "ntm_fluid_tank", 2, 1);
    }

    protected abstract FluidTankNTM tank(BE machine);

    @Override
    protected final void capture(BE machine) {
        FluidTankNTM tank = tank(machine);
        put(FILL, tank.getFill());
        put(MAX, tank.getMaxFill());
        putRef(TYPE, NTMFluids.legacyName(tank.getTankType()));
    }

    @LuaFunction
    public final Object[] getFluidStored() {
        return read(s -> new Object[] {s.intAt(FILL)});
    }

    @LuaFunction
    public final Object[] getMaxStored() {
        return read(s -> new Object[] {s.intAt(MAX)});
    }

    @LuaFunction
    public final Object[] getTypeStored() {
        return read(s -> new Object[] {s.refAt(TYPE)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(s -> new Object[] {s.intAt(FILL), s.intAt(MAX), s.refAt(TYPE)});
    }
}
