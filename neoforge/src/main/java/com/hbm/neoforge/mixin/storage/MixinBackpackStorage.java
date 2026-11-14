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

package com.hbm.neoforge.mixin.storage;

import com.hbm.neoforge.inventory.SophisticatedContentsStorage;
import com.hbm.neoforge.inventory.SophisticatedItemInteriors;
import java.util.Map;
import java.util.UUID;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackStorage;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BackpackStorage.class, remap = false)
public abstract class MixinBackpackStorage implements SophisticatedContentsStorage {
    @Shadow @Final private Map<UUID, ContainerContents> backpackContents;

    @Override
    public @Nullable ContainerContents storedContents(UUID id) {
        return backpackContents.get(id);
    }

    @Inject(method = "getOrCreateBackpackContents", at = @At("HEAD"), cancellable = true)
    private void hbm$existingContents(UUID id, CallbackInfoReturnable<ContainerContents> ci) {
        ContainerContents current = backpackContents.get(id);
        if (current != null) ci.setReturnValue(current);
    }

    @Inject(method = "getOrCreateBackpackContents", at = @At("RETURN"))
    private void hbm$createdContents(UUID id, CallbackInfoReturnable<ContainerContents> ci) {
        SophisticatedItemInteriors.backpackRecordChanged(id);
    }

    @Inject(method = "setBackpackContents", at = @At("RETURN"))
    private void hbm$recordReplaced(UUID id, ContainerContents contents, CallbackInfo ci) {
        SophisticatedItemInteriors.backpackRecordChanged(id);
    }

    @Inject(method = "removeBackpackContents", at = @At("RETURN"))
    private void hbm$recordRemoved(UUID id, CallbackInfo ci) {
        SophisticatedItemInteriors.backpackRecordChanged(id);
    }
}
