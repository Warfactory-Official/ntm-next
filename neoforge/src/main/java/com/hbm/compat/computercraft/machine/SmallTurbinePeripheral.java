// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.machine;

import com.hbm.compat.computercraft.SteamTypes;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.BlockEntityMachineTurbine;
import dan200.computercraft.api.lua.LuaFunction;

public final class SmallTurbinePeripheral extends TurbinePeripheral<BlockEntityMachineTurbine> {

    public SmallTurbinePeripheral(BlockEntityMachineTurbine machine) {
        super(machine, SLOTS);
    }

    @Override
    protected FluidTankNTM input(BlockEntityMachineTurbine machine) {
        return machine.tank0;
    }

    @Override
    protected FluidTankNTM output(BlockEntityMachineTurbine machine) {
        return machine.tank1;
    }

    @Override
    protected long power(BlockEntityMachineTurbine machine) {
        return machine.power;
    }

    @LuaFunction(mainThread = true)
    public final Object[] setType(int type) {
        machine().tank0.setTankType(SteamTypes.fromInt(type));
        return new Object[] {true};
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
