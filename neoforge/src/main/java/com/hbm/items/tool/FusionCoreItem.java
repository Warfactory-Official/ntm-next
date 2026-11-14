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

import com.hbm.api.energymk2.EnergyItem;
import com.hbm.handler.ArmorUtil;
import com.hbm.items.armor.ArmorSuitEffects;
import com.hbm.items.armor.ModArmorItem;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import com.hbm.util.NumberFormatter;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class FusionCoreItem extends Item {

    private final long charge;

    public FusionCoreItem(Properties properties, long charge) {
        super(properties);
        this.charge = charge;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ModArmorItem plate)
                || !plate.isPowered()
                || !ArmorSuitEffects.hasFullSet(player, plate.suit(), false)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            for (EquipmentSlot slot : ArmorUtil.ARMOR_SLOTS) {
                ItemStack worn = player.getItemBySlot(slot);
                if (worn.getItem() instanceof ModArmorItem armor) {
                    armor.setCharge(worn, armor.getCharge(worn) + charge);
                } else if (worn.getItem() instanceof EnergyItem battery) {
                    battery.addCharge(worn, charge);
                }
            }
            stack.shrink(1);
        }
        EntitySounds.play(
                player,
                player.getX(),
                player.getY(),
                player.getZ(),
                ModSounds.ITEM_BATTERY.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> lines,
            TooltipFlag flag) {
        lines.accept(
                Component.translatable(
                                "desc.item.fusionCore.charges",
                                NumberFormatter.formatCompact(charge))
                        .withStyle(ChatFormatting.YELLOW));
        lines.accept(Component.translatable("desc.item.fusionCore.requiresSet"));
        super.appendHoverText(stack, context, display, lines, flag);
    }
}
