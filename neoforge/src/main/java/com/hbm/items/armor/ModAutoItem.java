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

package com.hbm.items.armor;

import com.hbm.extprop.LivingHazards;
import com.hbm.handler.ArmorModHandler;
import com.hbm.potion.HbmPotion;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class ModAutoItem extends ArmorModItem {

    private static final float TRIGGER = 5F;
    private static final int STABILITY_DURATION = 60 * SharedConstants.TICKS_PER_SECOND;

    public ModAutoItem(Properties properties) {
        super(properties, ArmorModHandler.EXTRA, false, true, false, false);
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        adder.accept(
                Component.translatable("desc.item.armorMod.auto").withStyle(ChatFormatting.BLUE));
    }

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(
                Component.translatable("desc.item.armorMod.auto.installed", stack.getHoverName())
                        .withStyle(ChatFormatting.BLUE));
    }

    @Override
    public boolean ticksServer() {
        return true;
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (entity.level().isClientSide() || LivingHazards.digamma(entity) < TRIGGER) return;

        ArmorModHandler.removeMod(armor, ArmorModHandler.EXTRA);
        EntitySounds.play(
                entity,
                entity.blockPosition(),
                ModSounds.ITEM_SYRINGE.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F);
        LivingHazards.setDigamma(entity, LivingHazards.digamma(entity) - TRIGGER);
        entity.addEffect(new MobEffectInstance(HbmPotion.stability(), STABILITY_DURATION, 0));
        entity.heal(20F);
    }
}
