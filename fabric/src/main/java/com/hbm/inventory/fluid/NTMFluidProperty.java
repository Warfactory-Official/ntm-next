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

import com.hbm.inventory.fluid.trait.FluidTrait;
import com.hbm.inventory.fluid.trait.FluidTraitSimple;
import java.util.List;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public record NTMFluidProperty(
        int color,
        HazardSymbol symbol,
        int temperature,
        int nfpaHealth,
        int nfpaFlame,
        int nfpaReact,
        FluidTrait[] traits,
        List<TagKey<Fluid>> shares) {

    public boolean hasTrait(Class<? extends FluidTrait> trait) {
        for (FluidTrait t : traits) {
            if (trait.isInstance(t)) return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public <T extends FluidTrait> @Nullable T getTrait(Class<? extends T> trait) {
        for (FluidTrait t : traits) {
            if (trait.isInstance(t)) return (T) t;
        }
        return null;
    }

    public int colorARGB() {
        return ARGB.opaque(color);
    }

    public boolean isDispersable() {
        return !(hasTrait(FluidTraitSimple.FT_Amat.class)
                || hasTrait(FluidTraitSimple.FT_NoContainer.class)
                || hasTrait(FluidTraitSimple.FT_Viscous.class));
    }
}
