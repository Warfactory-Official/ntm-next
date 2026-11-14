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

package com.hbm.items.tool;

import com.hbm.lib.ModDamageTypes;
import com.hbm.potion.HbmPotion;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public final class GavelItem extends Item {

    private final Type type;

    public GavelItem(Properties properties, Type type) {
        super(properties);
        this.type = type;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!(target.level() instanceof ServerLevel level)) return;

        if (type == Type.WOOD) {
            EntitySounds.play(
                    target,
                    target.blockPosition(),
                    ModSounds.GAVEL.get(),
                    SoundSource.PLAYERS,
                    3.0F,
                    1.0F);
        } else if (type == Type.LEAD) {
            EntitySounds.play(
                    target,
                    target.blockPosition(),
                    ModSounds.GAVEL.get(),
                    SoundSource.PLAYERS,
                    3.0F,
                    1.0F);
            target.addEffect(new MobEffectInstance(HbmPotion.lead(), 15 * 20, 4));
        } else {
            target.hurtServer(
                    level,
                    level.damageSources().source(ModDamageTypes.HEALTH_DRAIN, attacker),
                    target.getMaxHealth() / 3.0F);
            EntitySounds.play(
                    target,
                    target.blockPosition(),
                    ModSounds.GAVEL.get(),
                    SoundSource.PLAYERS,
                    3.0F,
                    1.0F);
        }
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        if (type == Type.WOOD) {
            adder.accept(Component.translatable("desc.item.gavel.thunk"));
        } else if (type == Type.LEAD) {
            adder.accept(Component.translatable("desc.item.gavel.youAreHerebySentenced"));
        } else {
            adder.accept(Component.translatable("desc.item.gavel.theJokeItMakes"));
            adder.accept(Component.empty());
            adder.accept(
                    Component.translatable("desc.item.gavel.dealsAsMuchDamage")
                            .withStyle(ChatFormatting.BLUE));
        }
    }

    public enum Type {
        WOOD,
        LEAD,
        DIAMOND
    }
}
