// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.machine;

import com.hbm.blockentity.machine.BlockEntityMachineLargeTurbine;
import com.hbm.compat.computercraft.SteamTypes;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import dan200.computercraft.api.lua.LuaFunction;

public final class LargeTurbinePeripheral
        extends TurbinePeripheral<BlockEntityMachineLargeTurbine> {

    public LargeTurbinePeripheral(BlockEntityMachineLargeTurbine machine) {
        super(machine, SLOTS);
    }

    @Override
    protected FluidTankNTM input(BlockEntityMachineLargeTurbine machine) {
        return machine.tanks[0];
    }

    @Override
    protected FluidTankNTM output(BlockEntityMachineLargeTurbine machine) {
        return machine.tanks[1];
    }

    @Override
    protected long power(BlockEntityMachineLargeTurbine machine) {
        return machine.power;
    }

    @LuaFunction(mainThread = true)
    public final Object[] setType(int type) {
        BlockEntityMachineLargeTurbine machine = machine();
        machine.tanks[0].setTankType(SteamTypes.fromInt(type));
        machine.markChanged();
        return new Object[] {};
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
                            s.longAt(POWER)
                        });
    }
}
