// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import com.hbm.blockentity.machine.storage.BlockEntityMassStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class MassStorageMEInventory implements OwnedMEStorage {
    private final BlockEntityMassStorage owner;

    public MassStorageMEInventory(BlockEntityMassStorage owner) {
        this.owner = owner;
    }

    @Override
    public BlockEntity physicalOwner() {
        return owner;
    }

    @Override
    public boolean isPreferredStorageFor(AEKey what, IActionSource source) {
        return !owner.isRemoved()
                && AEItemKey.matches(what, owner.getItem(BlockEntityMassStorage.SLOT_TYPE));
    }

    @Override
    public long insertPhysical(AEKey what, long amount, Actionable mode, IActionSource source) {
        assert amount >= 0;
        if (!isPreferredStorageFor(what, source)) return 0;
        return amount - owner.increaseTotalStockpile(amount, mode == Actionable.MODULATE);
    }

    @Override
    public long extractPhysical(AEKey what, long amount, Actionable mode, IActionSource source) {
        assert amount >= 0;
        if (!isPreferredStorageFor(what, source)) return 0;
        return amount - owner.decreaseTotalStockpile(amount, mode == Actionable.MODULATE);
    }

    @Override
    public void listPhysical(KeyCounter out) {
        if (owner.isRemoved()) return;
        long amount = owner.getTotalStockpile();
        if (amount > 0)
            AE2StorageQueries.add(
                    this,
                    out,
                    AEItemKey.of(owner.getItem(BlockEntityMassStorage.SLOT_TYPE)),
                    amount);
    }

    @Override
    public Component getDescription() {
        return owner.getDisplayName();
    }
}
