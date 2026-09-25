// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.machine;

import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.BlockEntityMachineIndustrialTurbine;
import dan200.computercraft.api.lua.LuaFunction;

public final class IndustrialTurbinePeripheral
        extends TurbinePeripheral<BlockEntityMachineIndustrialTurbine> {

    private static final int FLYWHEEL = SLOTS;

    public IndustrialTurbinePeripheral(BlockEntityMachineIndustrialTurbine machine) {
        super(machine, SLOTS + 1);
    }

    @Override
    protected FluidTankNTM input(BlockEntityMachineIndustrialTurbine machine) {
        return machine.tanks[0];
    }

    @Override
    protected FluidTankNTM output(BlockEntityMachineIndustrialTurbine machine) {
        return machine.tanks[1];
    }

    @Override
    protected long power(BlockEntityMachineIndustrialTurbine machine) {
        return machine.powerBuffer;
    }

    @Override
    protected void capture(BlockEntityMachineIndustrialTurbine machine) {
        super.capture(machine);
        put(FLYWHEEL, (int) (machine.spin * 100));
    }

    @LuaFunction
    public final Object[] getFlywheel() {
        return read(s -> new Object[] {s.intAt(FLYWHEEL)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(
                s ->
                        new Object[] {
                            s.intAt(IN_FILL),
                            s.intAt(IN_MAX),
                            s.intAt(OUT_FILL),
                            s.intAt(OUT_MAX),
                            s.intAt(STEAM),
                            s.longAt(POWER),
                            s.intAt(FLYWHEEL)
                        });
    }
}
