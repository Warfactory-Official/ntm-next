/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.items.food;

import com.hbm.api.energymk2.EnergyItem;
import com.hbm.handler.ArmorUtil;
import com.hbm.items.ModItems;
import com.hbm.items.armor.ArmorSuitEffects;
import com.hbm.items.armor.ModArmorItem;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class PancakeItem extends Item {

    public static final FoodProperties FOOD =
            new FoodProperties.Builder()
                    .nutrition(20)
                    .saturationModifier(20F)
                    .alwaysEdible()
                    .build();

    public PancakeItem(Properties props) {
        super(props);
    }

    private static boolean canChewIt(Player player) {
        return ArmorSuitEffects.hasFullSet(player, ModArmorItem.Suit.BJ, false)
                && player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.BJ_HELMET.get());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (canChewIt(player)) return super.use(level, player, hand);
        if (!level.isClientSide()) {
            player.sendSystemMessage(
                    Component.translatable("desc.item.pancake.yourTeethAreToo")
                            .withStyle(ChatFormatting.YELLOW));
        }
        return InteractionResult.PASS;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        for (EquipmentSlot slot : ArmorUtil.ARMOR_SLOTS) {
            ItemStack worn = entity.getItemBySlot(slot);
            if (worn.getItem() instanceof EnergyItem battery) {
                battery.setCharge(worn, battery.getMaxCharge(worn));
            }
        }
        return result;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag tooltipFlag) {
        adder.accept(Component.translatable("desc.item.pancake.canBeEatenTo"));
        adder.accept(Component.translatable("desc.item.pancake.notForPeopleWith"));
        adder.accept(Component.empty());
        adder.accept(Component.translatable("desc.item.pancake.halfBurntAndSmells"));
    }
}
