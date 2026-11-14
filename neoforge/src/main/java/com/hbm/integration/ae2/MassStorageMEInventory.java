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
import com.hbm.blockentity.machine.storage.MassStorageBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class MassStorageMEInventory implements OwnedMEStorage {
    private final MassStorageBlockEntity owner;

    public MassStorageMEInventory(MassStorageBlockEntity owner) {
        this.owner = owner;
    }

    @Override
    public BlockEntity physicalOwner() {
        return owner;
    }

    @Override
    public boolean isPreferredStorageFor(AEKey what, IActionSource source) {
        return !owner.isRemoved()
                && AEItemKey.matches(what, owner.getItem(MassStorageBlockEntity.SLOT_TYPE));
    }

    @Override
    public long insertPhysical(AEKey what, long amount, Actionable mode, IActionSource source) {
        assert amount >= 0;
        if (!isPreferredStorageFor(what, source)) return 0;
        return amount - owner.addStock(amount, mode == Actionable.MODULATE);
    }

    @Override
    public long extractPhysical(AEKey what, long amount, Actionable mode, IActionSource source) {
        assert amount >= 0;
        if (!isPreferredStorageFor(what, source)) return 0;
        return amount - owner.removeStock(amount, mode == Actionable.MODULATE);
    }

    @Override
    public void listPhysical(KeyCounter out) {
        if (owner.isRemoved()) return;
        long amount = owner.getTotalStock();
        if (amount > 0)
            AE2StorageQueries.add(
                    this,
                    out,
                    AEItemKey.of(owner.getItem(MassStorageBlockEntity.SLOT_TYPE)),
                    amount);
    }

    @Override
    public Component getDescription() {
        return owner.getDisplayName();
    }
}
