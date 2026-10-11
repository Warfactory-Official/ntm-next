// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.fluidmk2;

import com.hbm.platform.FabricCaps;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.world.level.material.Fluid;

final class ForeignFluidEndpoint implements IFluidHandlerMK2 {

    Storage<FluidVariant> store;
    private long owed;

    ForeignFluidEndpoint(Storage<FluidVariant> store) {
        this.store = store;
    }

    long extractable(Fluid type) {
        return Math.max(0, FabricCaps.extractable(store, type) - owed);
    }

    long insertable(Fluid type) {
        return FabricCaps.insertable(store, type);
    }

    @Override
    public long getFluidAvailable(Fluid type, int pressure) {
        return pressure == 0 ? extractable(type) : 0L;
    }

    @Override
    public void useUpFluid(Fluid type, int pressure, long amount) {
        if (pressure != 0 || amount <= 0) return;
        long want = amount + owed;
        owed = want - FabricCaps.moveWholeMb(store, FluidVariant.of(type), want, false);
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        return pressure == 0 ? FabricCaps.insertable(store, type) : 0L;
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        return pressure == 0
                ? amount - FabricCaps.moveWholeMb(store, FluidVariant.of(type), amount, true)
                : amount;
    }
}
