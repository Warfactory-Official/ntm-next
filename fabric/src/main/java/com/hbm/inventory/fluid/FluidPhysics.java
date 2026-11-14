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

import com.hbm.inventory.fluid.trait.FluidTraitSimple;
import net.minecraft.world.level.material.Fluid;

public record FluidPhysics(int temperature, boolean gaseous, boolean viscous) {

    public static final int WATER_TEMPERATURE = 300;
    public static final int WATER_VISCOSITY = 1000;
    public static final int GAS_VISCOSITY = 500;
    public static final int VISCOUS_VISCOSITY = 2000;
    public static final int WATER_DENSITY = 1000;
    public static final int GAS_DENSITY = -500;

    public static FluidPhysics of(Fluid fluid) {
        NTMFluidProperty property = NTMFluidProperties.get(fluid);
        if (property == null) return new FluidPhysics(0, false, false);
        return new FluidPhysics(
                property.temperature(),
                property.hasTrait(FluidTraitSimple.FT_Gaseous.class)
                        || property.hasTrait(FluidTraitSimple.FT_Plasma.class),
                property.hasTrait(FluidTraitSimple.FT_Viscous.class));
    }

    public int kelvin() {
        return temperature == 0 ? WATER_TEMPERATURE : 273 + temperature;
    }

    public int density() {
        return gaseous ? GAS_DENSITY : WATER_DENSITY;
    }

    public int viscosity() {
        if (gaseous) return GAS_VISCOSITY;
        return viscous ? VISCOUS_VISCOSITY : WATER_VISCOSITY;
    }
}
