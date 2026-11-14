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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "appeng.me.storage.CompositeStorage$InventoryCache", remap = false)
public abstract class MixinInventoryCache {
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
