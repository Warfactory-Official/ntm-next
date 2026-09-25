// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.storage;

import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.storage.BlockEntityMachineFluidTank;

public final class MachineFluidTankPeripheral
        extends FluidTankPeripheral<BlockEntityMachineFluidTank> {

    public MachineFluidTankPeripheral(BlockEntityMachineFluidTank machine) {
        super(machine);
    }

    @Override
    protected FluidTankNTM tank(BlockEntityMachineFluidTank machine) {
        return machine.tank;
    }
}
