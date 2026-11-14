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

import appeng.blockentity.crafting.CraftingBlockEntity;
import appeng.crafting.execution.CraftingCpuLogic;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.hbm.inventory.InventoryChanges;
import com.hbm.neoforge.inventory.AE2CpuAnchor;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CraftingCPUCluster.class, remap = false)
public abstract class MixinCraftingCPUCluster implements AE2CpuAnchor {
    @Shadow @Final public CraftingCpuLogic craftingLogic;

    @Shadow
    private CraftingBlockEntity getCore() {
        throw new AssertionError();
    }

    @ModifyReturnValue(method = "isBusy", at = @At("RETURN"))
    private boolean hbm$pendingReturns(boolean busy) {
        return busy || !craftingLogic.getInventory().list.isEmpty();
    }

    @Override
    public BlockEntity craftingInventoryAnchor() {
        return getCore();
    }

    @Inject(method = "done", at = @At("RETURN"))
    private void hbm$formedAnchor(CallbackInfo ci) {
        InventoryChanges.storageLayoutChanged(getCore());
    }
}
