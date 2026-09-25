// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class NeoForgeFluidHandlerView implements IFluidHandlerView {

    private final ResourceHandler<FluidResource> handler;

    NeoForgeFluidHandlerView(ResourceHandler<FluidResource> handler) {
        this.handler = handler;
    }

    private static int clampInt(long mb) {
        return mb > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) mb;
    }

    @Override
    public Object handlerIdentity() {
        return handler;
    }

    @Override
    public long extractable(Fluid fluid) {
        FluidResource res = FluidResource.of(fluid);
        try (Transaction tx = Transaction.openRoot()) {
            return handler.extract(res, Integer.MAX_VALUE, tx);
        }
    }

    @Override
    public long extract(Fluid fluid, long maxMb) {
        FluidResource res = FluidResource.of(fluid);
        try (Transaction tx = Transaction.openRoot()) {
            int out = handler.extract(res, clampInt(maxMb), tx);
            tx.commit();
            return out;
        }
    }

    @Override
    public long insertable(Fluid fluid) {
        FluidResource res = FluidResource.of(fluid);
        try (Transaction tx = Transaction.openRoot()) {
            return handler.insert(res, Integer.MAX_VALUE, tx);
        }
    }

    @Override
    public long insert(Fluid fluid, long maxMb) {
        FluidResource res = FluidResource.of(fluid);
        try (Transaction tx = Transaction.openRoot()) {
            int in = handler.insert(res, clampInt(maxMb), tx);
            tx.commit();
            return in;
        }
    }
}
