// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.fluidmk2;

import com.hbm.platform.ICapabilityService;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public record FluidFace(@Nullable Direction side, @Nullable Fluid fluid)
        implements ICapabilityService.FacedContext {

    public static FluidFace of(@Nullable Direction side, @Nullable Fluid fluid) {
        return new FluidFace(side, fluid);
    }

    public static FluidFace any(@Nullable Direction side) {
        return new FluidFace(side, null);
    }
}
