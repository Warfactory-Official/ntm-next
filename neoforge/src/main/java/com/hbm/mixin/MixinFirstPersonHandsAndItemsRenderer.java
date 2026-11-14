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

import com.hbm.items.tool.ChainsawItem;
import com.hbm.items.weapon.CrucibleItem;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.item.component.SwingAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemInHandRenderer.class)
public abstract class MixinFirstPersonHandsAndItemsRenderer {

    @Unique
    private static final SwingAnimation HBM$NO_SWING =
            new SwingAnimation(SwingAnimationType.NONE, 1);

    @ModifyExpressionValue(
            method = "submitArmWithItem",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/ItemStack;getSwingAnimation()Lnet/minecraft/world/item/component/SwingAnimation;"))
    private SwingAnimation hbm$toolFirstPersonSwing(
            SwingAnimation swing, @Local(argsOnly = true) ItemStack itemStack) {
        return itemStack.getItem() instanceof ChainsawItem
                        || itemStack.getItem() instanceof CrucibleItem
                ? HBM$NO_SWING
                : swing;
    }
}
