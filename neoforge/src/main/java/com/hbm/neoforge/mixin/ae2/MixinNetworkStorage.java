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

package com.hbm.neoforge.mixin.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.me.storage.NetworkStorage;
import com.hbm.integration.ae2.AE2StorageQueries;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = NetworkStorage.class, remap = false)
public abstract class MixinNetworkStorage {
    @Shadow private boolean mountsInUse;

    @WrapMethod(method = "getAvailableStacks(Lappeng/api/stacks/KeyCounter;)V")
    private void hbm$listOwnedStoresOnce(KeyCounter out, Operation<Void> original) {
        if (mountsInUse) {
            original.call(out);
            return;
        }
        AE2StorageQueries.beginListing(out);
        try {
            original.call(out);
        } finally {
            AE2StorageQueries.end();
        }
    }

    @WrapMethod(method = "insert")
    private long hbm$shareInsertSimulation(
            AEKey what,
            long amount,
            Actionable mode,
            IActionSource source,
            Operation<Long> original) {
        if (mode != Actionable.SIMULATE || mountsInUse)
            return original.call(what, amount, mode, source);
        AE2StorageQueries.beginSimulation(what, true);
        try {
            return original.call(what, amount, mode, source);
        } finally {
            AE2StorageQueries.end();
        }
    }

    @WrapMethod(method = "extract")
    private long hbm$shareExtractSimulation(
            AEKey what,
            long amount,
            Actionable mode,
            IActionSource source,
            Operation<Long> original) {
        if (mode != Actionable.SIMULATE || mountsInUse)
            return original.call(what, amount, mode, source);
        AE2StorageQueries.beginSimulation(what, false);
        try {
            return original.call(what, amount, mode, source);
        } finally {
            AE2StorageQueries.end();
        }
    }
}
