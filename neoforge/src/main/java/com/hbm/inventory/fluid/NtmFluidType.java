/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

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
