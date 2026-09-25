// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.fluid;

import net.neoforged.neoforge.fluids.FluidType;

public final class InertFluidType extends FluidType {

    private final ClassicFluid.Spec spec;

    public InertFluidType(ClassicFluid.Spec spec) {
        super(
                Properties.create()
                        .canSwim(false)
                        .canDrown(false)
                        .pathType(null)
                        .adjacentPathType(null));
        this.spec = spec;
    }

    @Override
    public String getDescriptionId() {
        return spec.block().get().getDescriptionId();
    }
}
