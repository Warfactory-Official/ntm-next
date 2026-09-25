// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid;

import com.hbm.lib.Library;
import java.util.function.Supplier;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidType;

public final class NtmFluidType extends FluidType {

    private final Supplier<Fluid> fluid;

    public NtmFluidType(Supplier<Fluid> fluid, String path) {
        super(
                FluidType.Properties.create()
                        .descriptionId(NTMFluidProperties.nameKey(Library.id(path))));
        this.fluid = fluid;
    }

    @Override
    public int getTemperature() {
        return FluidPhysics.of(fluid.get()).kelvin();
    }

    @Override
    public int getDensity() {
        return FluidPhysics.of(fluid.get()).density();
    }

    @Override
    public int getViscosity() {
        return FluidPhysics.of(fluid.get()).viscosity();
    }
}
