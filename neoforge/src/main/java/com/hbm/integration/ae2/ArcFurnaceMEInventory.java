/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.integration.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import com.hbm.blockentity.processing.ArcFurnaceBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class ArcFurnaceMEInventory implements OwnedMEStorage {
    private final ArcFurnaceBlockEntity owner;

    public ArcFurnaceMEInventory(ArcFurnaceBlockEntity owner) {
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
        for (int slot = 0; slot <= ArcFurnaceBlockEntity.SLOT_GRID_END; slot++) {
            ItemStack stack = owner.getAvailableItemFromSlot(slot);
            if (!stack.isEmpty())
                AE2StorageQueries.add(this, out, AEItemKey.of(stack), stack.getCount());
        }
    }

    @Override
    public int getEstimatedStackCount() {
        return 3 + ArcFurnaceBlockEntity.SLOT_GRID_END - ArcFurnaceBlockEntity.SLOT_GRID_START + 1;
    }

    @Override
    public Component getDescription() {
        return owner.getDisplayName();
    }
}
