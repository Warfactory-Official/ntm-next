// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import com.hbm.blockentity.machine.BlockEntityMachineArcFurnace;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class ArcFurnaceMEInventory implements OwnedMEStorage {
    private final BlockEntityMachineArcFurnace owner;

    public ArcFurnaceMEInventory(BlockEntityMachineArcFurnace owner) {
        this.owner = owner;
    }

    @Override
    public BlockEntity physicalOwner() {
        return owner;
    }

    @Override
    public long insertPhysical(AEKey what, long amount, Actionable mode, IActionSource source) {
        assert amount >= 0;
        if (owner.isRemoved() || !(what instanceof AEItemKey item)) return 0;
        return owner.distributeInput(item.getReadOnlyStack(), amount, mode == Actionable.MODULATE);
    }

    @Override
    public long extractPhysical(AEKey what, long amount, Actionable mode, IActionSource source) {
        assert amount >= 0;
        if (owner.isRemoved() || !(what instanceof AEItemKey item)) return 0;
        return owner.collectRequested(item.getReadOnlyStack(), amount, mode == Actionable.MODULATE);
    }

    @Override
    public void listPhysical(KeyCounter out) {
        if (owner.isRemoved()) return;
        for (int slot = 0; slot <= BlockEntityMachineArcFurnace.SLOT_GRID_END; slot++) {
            ItemStack stack = owner.getAvailableItemFromSlot(slot);
            if (!stack.isEmpty())
                AE2StorageQueries.add(this, out, AEItemKey.of(stack), stack.getCount());
        }
    }

    @Override
    public int getEstimatedStackCount() {
        return 3
                + BlockEntityMachineArcFurnace.SLOT_GRID_END
                - BlockEntityMachineArcFurnace.SLOT_GRID_START
                + 1;
    }

    @Override
    public Component getDescription() {
        return owner.getDisplayName();
    }
}
