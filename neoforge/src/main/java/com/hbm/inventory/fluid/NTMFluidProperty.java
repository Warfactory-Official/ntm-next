// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
        EnumSymbol symbol,
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
