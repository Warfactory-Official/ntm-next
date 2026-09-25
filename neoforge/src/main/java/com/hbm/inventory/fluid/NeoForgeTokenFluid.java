// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid;

import java.util.function.Supplier;
import net.neoforged.neoforge.fluids.FluidType;

public final class NeoForgeTokenFluid extends NTMTokenFluidBase {

    private final Supplier<? extends FluidType> typeSupplier;

    public NeoForgeTokenFluid(Supplier<? extends FluidType> typeSupplier) {
        this.typeSupplier = typeSupplier;
    }

    @Override
    public FluidType getFluidType() {
        return typeSupplier.get();
    }
}
