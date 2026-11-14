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
