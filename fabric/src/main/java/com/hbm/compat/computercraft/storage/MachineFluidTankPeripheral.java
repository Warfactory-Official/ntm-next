// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.storage;

import com.hbm.blockentity.machine.storage.BlockEntityMachineFluidTank;
import com.hbm.inventory.fluid.tank.FluidTankNTM;

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
