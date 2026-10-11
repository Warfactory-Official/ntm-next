// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.mixin.ae2;

import appeng.api.stacks.KeyCounter;
import com.hbm.integration.ae2.AE2StorageQueries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KeyCounter.class, remap = false)
public abstract class MixinKeyCounter implements AE2StorageQueries.CounterMetadata {
    @Unique private AE2StorageQueries.Origins hbm$origins;

    @Override
    public AE2StorageQueries.Origins hbm$listingOrigins() {
        return hbm$origins;
    }

    @Override
    public void hbm$listingOrigins(AE2StorageQueries.Origins origins) {
        hbm$origins = origins;
    }

    @Inject(
            method = {"clear", "reset"},
            at = @At("HEAD"))
    private void hbm$clearListingOrigins(CallbackInfo ci) {
        if (hbm$origins != null) hbm$origins.clear();
    }
}
