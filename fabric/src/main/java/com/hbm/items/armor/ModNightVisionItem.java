/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

package com.hbm.items.armor;

import com.hbm.extprop.PlayerState;
import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.ArmorUtil;
import com.hbm.items.ModDataComponents;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ModNightVisionItem extends ArmorModItem {
    private static final int DURATION = 15 * SharedConstants.TICKS_PER_SECOND;

    public ModNightVisionItem(Properties properties) {
        super(properties, ArmorModHandler.HELMET_ONLY, true, false, false, false);
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        adder.accept(
                Component.translatable("desc.item.armorMod.nightVision")
                        .withStyle(ChatFormatting.AQUA));
    }

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(
                Component.translatable(
                                "desc.item.armorMod.nightVision.installed", stack.getHoverName())
                        .withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public boolean ticksServer() {
        return true;
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (entity.level().isClientSide() || !(entity instanceof Player player)) return;
        if (!(armor.getItem() instanceof ModArmorItem suit) || !suit.isPowered()) return;
        if (!ArmorSuitEffects.hasFullSet(player, suit.suit())) return;

        if (PlayerState.getOrCreate(player).enableHUD) {
            entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, DURATION, 0));
            if (!armor.has(ModDataComponents.ARMOR_MOD_NV_ACTIVE.get()))
                armor.set(ModDataComponents.ARMOR_MOD_NV_ACTIVE.get(), Unit.INSTANCE);
            if (entity.getRandom().nextInt(200) == 0)
                ArmorUtil.drainSupplyForWear(entity, armor, 1);
        } else if (armor.has(ModDataComponents.ARMOR_MOD_NV_ACTIVE.get())) {
            entity.removeEffect(MobEffects.NIGHT_VISION);
            armor.remove(ModDataComponents.ARMOR_MOD_NV_ACTIVE.get());
        }
    }
}
