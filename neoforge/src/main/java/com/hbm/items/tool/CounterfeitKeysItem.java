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

package com.hbm.items.tool;

import com.hbm.blockentity.security.LockableBlockEntity;
import com.hbm.items.ModItems;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class CounterfeitKeysItem extends Item {

    public CounterfeitKeysItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer().isSpectator()) return InteractionResult.PASS;
        Level level = context.getLevel();
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof LockableBlockEntity lock)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        Player player = context.getPlayer();
        if (lock.isLocked() && lock.allowsCounterfeitKeys()) {
            ItemStack key = new ItemStack(ModItems.KEY_FAKE);
            KeyPinItem.setPins(key, lock.getPinCode());

            player.setItemInHand(context.getHand(), key.copy());
            player.getInventory().placeItemBackInInventory(key.copy());
            ((LivingEntity) player).swing(context.getHand());
            return InteractionResult.SUCCESS;
        }
        if (!lock.allowsCounterfeitKeys()) {
            player.sendSystemMessage(
                    Component.translatable("desc.item.counterfeitKeys.thisLockIsToo")
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
            player.sendSystemMessage(
                    Component.translatable("desc.item.counterfeitKeys.perhapsThereIsAnother")
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag tooltipFlag) {
        adder.accept(Component.translatable("desc.item.counterfeitKeys.useOnALocked"));
    }
}
