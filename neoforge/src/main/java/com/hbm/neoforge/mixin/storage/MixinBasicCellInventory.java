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

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.me.cells.BasicCellInventory;
import com.hbm.integration.ae2.AE2ItemContents;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = BasicCellInventory.class, remap = false)
public abstract class MixinBasicCellInventory {
    @Shadow @Final private ItemStack i;

    @ModifyReturnValue(
            method = {"insert", "extract"},
            at = @At("RETURN"))
    private long hbm$publishArmedCellChange(
            long moved, AEKey key, long amount, Actionable mode, IActionSource source) {
        AE2ItemContents.persistArmedChange((BasicCellInventory) (Object) this, i, key, moved, mode);
        return moved;
    }
}
