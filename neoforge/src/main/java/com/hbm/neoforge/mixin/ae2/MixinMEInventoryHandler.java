// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.mixin.ae2;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.me.storage.MEInventoryHandler;
import com.hbm.integration.ae2.AE2StorageQueries;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
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

    @WrapOperation(
            method = "getAvailableStacks(Lappeng/api/stacks/KeyCounter;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lappeng/api/storage/MEStorage;getAvailableStacks()Lappeng/api/stacks/KeyCounter;"))
    private KeyCounter hbm$captureFilteredSource(
            MEStorage delegate, Operation<KeyCounter> original) {
        KeyCounter source = original.call(delegate);
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
