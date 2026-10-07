// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid;

import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidType;

public final class NtmFluidType extends FluidType {

    private final Supplier<Fluid> fluid;

    public NtmFluidType(Supplier<Fluid> fluid, Identifier id) {
        super(FluidType.Properties.create().descriptionId(NTMFluidProperties.nameKey(id)));
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
