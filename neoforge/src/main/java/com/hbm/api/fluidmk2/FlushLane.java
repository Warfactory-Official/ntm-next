// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.fluidmk2;

import com.hbm.inventory.fluid.tank.FluidTankNTM;
import java.util.function.BooleanSupplier;

public record FlushLane(
        IFluidHandlerMK2 provider,
        FluidTankNTM tank,
        FlushFaces faces,
        int period,
        BooleanSupplier enabled) {

    private static final BooleanSupplier ALWAYS = () -> true;

    public FlushLane(IFluidHandlerMK2 provider, FluidTankNTM tank, FlushFaces faces) {
        this(provider, tank, faces, 1, ALWAYS);
    }

    public FlushLane every(int ticks) {
        return new FlushLane(provider, tank, faces, ticks, enabled);
    }

    public FlushLane onlyWhen(BooleanSupplier gate) {
        return new FlushLane(provider, tank, faces, period, gate);
    }
}
