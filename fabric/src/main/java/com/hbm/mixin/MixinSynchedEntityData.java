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

package com.hbm.mixin;

import com.hbm.handler.UnstableFuses;
import com.hbm.inventory.InventorySlotBinding;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.syncher.SyncedDataHolder;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SynchedEntityData.class)
public abstract class MixinSynchedEntityData {
    @Shadow @Final private SyncedDataHolder entity;

    @WrapOperation(
            method = "set(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;Z)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/network/syncher/SynchedEntityData$DataItem;setValue(Ljava/lang/Object;)V"))
    private <T> void hbm$storedStackChanged(
            SynchedEntityData.DataItem<T> item, T value, Operation<Void> original) {
        if (item.getValue() instanceof ItemStack previous && entity instanceof Entity owner)
            InventorySlotBinding.unbind(previous, owner);
        original.call(item, value);
        if (value instanceof ItemStack stack && entity instanceof Entity owner)
            UnstableFuses.storedStackChanged(owner, stack);
    }
}
