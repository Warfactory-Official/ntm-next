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

import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.me.storage.MEInventoryHandler;
import com.hbm.integration.ae2.AE2StorageQueries;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = MEInventoryHandler.class, remap = false)
public abstract class MixinMEInventoryHandler {
    public KeyCounter getAvailableStacks() {
        MEStorage storage = (MEStorage) (Object) this;
        KeyCounter out = new KeyCounter(storage.getEstimatedStackCount());
        storage.getAvailableStacks(out);
        return out;
    }

    @WrapMethod(method = "getAvailableStacks(Lappeng/api/stacks/KeyCounter;)V")
    private void hbm$listingProjection(KeyCounter out, Operation<Void> original) {
        AE2StorageQueries.beginListing(out);
        try {
            original.call(out);
        } finally {
            AE2StorageQueries.end();
        }
    }

    @ModifyExpressionValue(
            method = "getAvailableStacks(Lappeng/api/stacks/KeyCounter;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lappeng/api/storage/MEStorage;getAvailableStacks()Lappeng/api/stacks/KeyCounter;"))
    private KeyCounter hbm$captureFilteredSource(KeyCounter source) {
        AE2StorageQueries.capturedListing(source);
        return source;
    }

    @Redirect(
            method = "getAvailableStacks(Lappeng/api/stacks/KeyCounter;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lappeng/api/stacks/KeyCounter;add(Lappeng/api/stacks/AEKey;J)V"),
            require = 1)
    private void hbm$mergeFilteredAliases(KeyCounter out, AEKey what, long amount) {
        AE2StorageQueries.addFiltered(out, what, amount);
    }
}
