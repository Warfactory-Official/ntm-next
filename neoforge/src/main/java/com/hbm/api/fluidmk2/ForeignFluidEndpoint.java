// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.fluidmk2;

import com.hbm.platform.NeoForgeCaps;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

final class ForeignFluidEndpoint implements IFluidHandlerMK2 {

    ResourceHandler<FluidResource> store;
    private long owed;

    ForeignFluidEndpoint(ResourceHandler<FluidResource> store) {
        this.store = store;
    }

    long extractable(Fluid type) {
        return Math.max(0, NeoForgeCaps.extractable(store, type) - owed);
    }

    long insertable(Fluid type) {
        return NeoForgeCaps.insertable(store, type);
    }

    @Override
    public long getFluidAvailable(Fluid type, int pressure) {
        return pressure == 0 ? extractable(type) : 0L;
    }

    @Override
    public void useUpFluid(Fluid type, int pressure, long amount) {
        if (pressure != 0 || amount <= 0) return;
        long want = amount + owed;
        owed = want - NeoForgeCaps.extract(store, type, want);
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        return pressure == 0 ? NeoForgeCaps.insertable(store, type) : 0L;
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        return pressure == 0 ? amount - NeoForgeCaps.insert(store, type, amount) : amount;
    }
}
