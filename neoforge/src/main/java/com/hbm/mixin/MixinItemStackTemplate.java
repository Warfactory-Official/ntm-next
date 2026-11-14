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

import com.hbm.items.weapon.sedna.GunTimers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStackTemplate.class)
public abstract class MixinItemStackTemplate {
    @Unique public volatile long hbm$unstableDeadline = Long.MIN_VALUE;
    @Unique public volatile Object[] hbm$interiorReferences;
    @Unique public volatile boolean hbm$opaqueReferences;

    @Inject(method = "fromStack", at = @At("RETURN"), cancellable = true)
    private static void hbm$storeGunCounters(
            ItemStack stack, CallbackInfoReturnable<ItemStackTemplate> cir) {
        ItemStack stored = GunTimers.forSave(stack);
        if (stored != stack) {
            cir.setReturnValue(
                    new ItemStackTemplate(
                            stored.typeHolder(), stored.getCount(), stored.getComponentsPatch()));
        }
    }
}
