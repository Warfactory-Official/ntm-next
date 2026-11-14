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
