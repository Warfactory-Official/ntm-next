// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.fluidmk2;

import com.hbm.inventory.fluid.tank.FluidTankNTM;
import net.minecraft.core.BlockPos;

public interface FluidFlushSender extends IFluidHandlerMK2 {

    FluidTankNTM[] getSendingTanks();

    BlockPos getBlockPos();

    default void declareFlush(FlushLanes out) {
        for (FluidTankNTM tank : getSendingTanks()) out.add(tank, FlushFaces.own());
    }
}
