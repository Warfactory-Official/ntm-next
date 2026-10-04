// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.storage;

import com.hbm.blockentity.machine.storage.BlockEntityBarrel;
import com.hbm.inventory.fluid.tank.FluidTankNTM;

public final class BarrelPeripheral extends FluidTankPeripheral<BlockEntityBarrel> {

    public BarrelPeripheral(BlockEntityBarrel machine) {
        super(machine);
    }

    @Override
    protected FluidTankNTM tank(BlockEntityBarrel machine) {
        return machine.tank;
    }
}
