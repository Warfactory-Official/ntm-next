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

import com.hbm.items.tool.ToolAbilityItem;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CampfireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CampfireBlock.class)
public abstract class MixinCampfireBlock {
    @WrapOperation(
            method = "useItemOn",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean hbm$requireReadyTool(
            ItemStack stack, TagKey<Item> tag, Operation<Boolean> original) {
        return original.call(stack, tag) && hbm$canDouse(stack);
    }

    private static boolean hbm$canDouse(ItemStack stack) {
        return !(stack.getItem() instanceof ToolAbilityItem tool) || tool.canOperate(stack);
    }

    @WrapOperation(
            method = "useItemOn",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V"))
    private void hbm$payDousingCost(
            ItemStack stack,
            int amount,
            LivingEntity user,
            EquipmentSlot slot,
            Operation<Void> original,
            @Local(argsOnly = true) Player player) {
        var item = stack.getItem();
        original.call(stack, amount, user, slot);
        if (item instanceof ToolAbilityItem tool) tool.finishToolAction(stack, player);
    }
}
