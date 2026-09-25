// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.capability.port;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

public interface IPortHost {

    default @Nullable ItemPort itemAccess(BlockPos cell, Direction side) {
        return null;
    }

    default @Nullable FluidPort fluidAccess(BlockPos cell, Direction side) {
        return null;
    }

    default @Nullable EnergyPort energyAccess(BlockPos cell, Direction side) {
        return null;
    }

    @SuppressWarnings("unchecked")
    default <D> @Nullable D portAccess(PortDomain<D> domain, BlockPos cell, Direction side) {
        if (domain == PortDomain.ITEM) return (D) itemAccess(cell, side);
        if (domain == PortDomain.FLUID) return (D) fluidAccess(cell, side);
        if (domain == PortDomain.ENERGY) return (D) energyAccess(cell, side);
        return null;
    }
}
