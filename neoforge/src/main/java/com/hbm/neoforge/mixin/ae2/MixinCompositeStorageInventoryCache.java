// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.mixin.ae2;

import appeng.api.stacks.KeyCounter;
import com.hbm.integration.ae2.AE2StorageQueries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "appeng.me.storage.CompositeStorage$InventoryCache", remap = false)
public abstract class MixinCompositeStorageInventoryCache {
    @Redirect(
            method = "getAvailableKeys",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lappeng/api/stacks/KeyCounter;addAll(Lappeng/api/stacks/KeyCounter;)V"))
    private void hbm$replayListingOrigins(KeyCounter out, KeyCounter cached) {
        AE2StorageQueries.addAll(out, cached);
    }
}
