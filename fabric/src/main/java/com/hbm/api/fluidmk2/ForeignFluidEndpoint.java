// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.fluidmk2;

import com.hbm.platform.IFluidHandlerView;
import net.minecraft.world.level.material.Fluid;

final class ForeignFluidEndpoint implements IFluidHandlerMK2 {

    private final IFluidHandlerView view;

    ForeignFluidEndpoint(IFluidHandlerView view) {
        this.view = view;
    }

    @Override
    public long getFluidAvailable(Fluid type, int pressure) {
        return pressure == 0 ? view.extractable(type) : 0L;
    }

    @Override
    public void useUpFluid(Fluid type, int pressure, long amount) {
        if (pressure == 0) view.extract(type, amount);
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        return pressure == 0 ? view.insertable(type) : 0L;
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        return pressure == 0 ? amount - view.insert(type, amount) : amount;
    }
}
