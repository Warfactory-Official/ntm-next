// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.level.material.Fluid;

final class FabricFluidHandlerView implements IFluidHandlerView {

    private static final long DROPLETS_PER_MB = 81L;

    private static final long MAX_MB = Long.MAX_VALUE / DROPLETS_PER_MB;

    private final Storage<FluidVariant> storage;

    FabricFluidHandlerView(Storage<FluidVariant> storage) {
        this.storage = storage;
    }

    private static long droplets(long mb) {
        return Math.min(mb, MAX_MB) * DROPLETS_PER_MB;
    }

    private long moveWholeMb(FluidVariant variant, long maxMb, boolean insert) {
        long offered = droplets(maxMb);
        if (offered <= 0) return 0L;
        try (Transaction outer = Transaction.openOuter()) {
            long probed;
            try (Transaction probe = outer.openNested()) {
                probed =
                        insert
                                ? storage.insert(variant, offered, probe)
                                : storage.extract(variant, offered, probe);
            }
            long whole = probed / DROPLETS_PER_MB * DROPLETS_PER_MB;
            if (whole <= 0) return 0L;
            long moved =
                    insert
                            ? storage.insert(variant, whole, outer)
                            : storage.extract(variant, whole, outer);
            if (moved != whole) return 0L;
            outer.commit();
            return moved / DROPLETS_PER_MB;
        }
    }

    @Override
    public Object handlerIdentity() {
        return storage;
    }

    @Override
    public long extractable(Fluid fluid) {
        FluidVariant v = FluidVariant.of(fluid);
        try (Transaction tx = Transaction.openOuter()) {
            return storage.extract(v, Long.MAX_VALUE, tx) / DROPLETS_PER_MB;
        }
    }

    @Override
    public long extract(Fluid fluid, long maxMb) {
        return moveWholeMb(FluidVariant.of(fluid), maxMb, false);
    }

    @Override
    public long insertable(Fluid fluid) {
        FluidVariant v = FluidVariant.of(fluid);
        try (Transaction tx = Transaction.openOuter()) {
            return storage.insert(v, Long.MAX_VALUE, tx) / DROPLETS_PER_MB;
        }
    }

    @Override
    public long insert(Fluid fluid, long maxMb) {
        return moveWholeMb(FluidVariant.of(fluid), maxMb, true);
    }
}
