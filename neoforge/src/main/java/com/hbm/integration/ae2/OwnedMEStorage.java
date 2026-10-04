// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import net.minecraft.world.level.block.entity.BlockEntity;

public sealed interface OwnedMEStorage extends MEStorage
        permits MassStorageMEInventory, ArcFurnaceMEInventory {
    BlockEntity physicalOwner();

    long insertPhysical(AEKey what, long amount, Actionable mode, IActionSource source);

    long extractPhysical(AEKey what, long amount, Actionable mode, IActionSource source);

    void listPhysical(KeyCounter out);

    @Override
    default void getAvailableStacks(KeyCounter out) {
        boolean grouped = AE2StorageQueries.beginSourceListing(this, out);
        try {
            listPhysical(out);
        } finally {
            if (grouped) AE2StorageQueries.endSourceListing();
        }
    }

    @Override
    default long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        return mode == Actionable.SIMULATE
                ? AE2StorageQueries.simulate(this, what, amount, source, true)
                : insertPhysical(what, amount, mode, source);
    }

    @Override
    default long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        return mode == Actionable.SIMULATE
                ? AE2StorageQueries.simulate(this, what, amount, source, false)
                : extractPhysical(what, amount, mode, source);
    }
}
