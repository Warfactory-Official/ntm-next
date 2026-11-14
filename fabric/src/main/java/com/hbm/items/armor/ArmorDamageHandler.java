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
import com.hbm.extprop.PlayerState;
import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.PlayerShield;
import com.hbm.handler.resistance.DamageResistanceHandler;
import com.hbm.items.ModItems;
import com.hbm.lib.ModDamageTypes;
import com.hbm.lib.ModifiedDamageSource;
import com.hbm.sound.EntitySounds;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ArmorDamageHandler {
    private static final float DNS_EXPLOSION_MULT = 0.001F;

    private ArmorDamageHandler() {}

    public static boolean allowsAttack(LivingEntity entity, DamageSource source, float amount) {
        if (filterAttack(entity, source, amount) <= 0F) return false;
        float pierceDT = source instanceof ModifiedDamageSource modifiers ? modifiers.pierceDT : 0F;
        float pierceDR = source instanceof ModifiedDamageSource modifiers ? modifiers.pierceDR : 0F;
        return !DamageResistanceHandler.refuses(entity, source, amount, pierceDT, pierceDR);
    }

    public static float modifyAcceptedDamage(
            LivingEntity entity, DamageSource source, float amount) {
        if (entity instanceof Player player) amount = PlayerShield.absorb(player, amount);
        LivingHazards props = LivingHazards.getIfPresent(entity);
        if (props != null && props.contagion() > 0 && amount < 100F) amount *= 2F;
        amount = ArmorModHandler.modifyDamage(entity, source, amount);
        amount = filterHurt(entity, source, amount);
        if (source.is(ModDamageTypes.ELECTRIC)
                && entity.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ModArmorItem armor
                && armor.maxPower() > 0) {
            amount *= 5F;
        }
        float pierceDT = source instanceof ModifiedDamageSource modifiers ? modifiers.pierceDT : 0F;
        float pierceDR = source instanceof ModifiedDamageSource modifiers ? modifiers.pierceDR : 0F;
        return Math.max(
                0F,
                DamageResistanceHandler.reduceAndNotify(
                        entity, source, amount, pierceDT, pierceDR));
    }

    public static float filterAttack(LivingEntity entity, DamageSource source, float amount) {
        if (amount <= 0F || !(entity instanceof Player player)) return amount;

        if (!source.is(DamageTypeTags.BYPASSES_ARMOR)
                && is(player, EquipmentSlot.HEAD, ModItems.NOSSY_HAT.get())
                && amount <= HatItem.DAMAGE_THRESHOLD) {
            EntitySounds.play(
                    player,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.ITEM_BREAK.value(),
                    SoundSource.PLAYERS,
                    5F,
                    1.0F + player.getRandom().nextFloat() * 0.5F);
            return 0F;
        }

        if (wearsAll(
                player,
                ModItems.EUPHEMIUM_HELMET.get(),
                ModItems.EUPHEMIUM_PLATE.get(),
                ModItems.EUPHEMIUM_LEGS.get(),
                ModItems.EUPHEMIUM_BOOTS.get())) {
            plink(player);
            return 0F;
        }

        if (ArmorSuitEffects.hasFullSet(player, ModArmorItem.Suit.DNS)) {
            if (source.is(DamageTypeTags.IS_EXPLOSION)) return amount;
            plink(player);
            return 0F;
        }

        if (ArmorSuitEffects.hasFullSet(player, ModArmorItem.Suit.TRENCHMASTER)) {
            if (source.is(DamageTypeTags.IS_EXPLOSION) && source.getEntity() == player) return 0F;
            if (player.getRandom().nextInt(3) == 0) {
                plink(player);
                return 0F;
            }
        }

        return amount;
    }

    public static float filterHurt(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof Player player)) return amount;
        if (ArmorSuitEffects.hasFullSet(player, ModArmorItem.Suit.DNS)
                && source.is(DamageTypeTags.IS_EXPLOSION)) {
            amount *= DNS_EXPLOSION_MULT;
        }
        if (!source.is(DamageTypeTags.BYPASSES_ARMOR)
                && player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.NO9.get())) {
            amount = Math.max(0F, amount - 0.5F);
        }
        if (!source.is(DamageTypeTags.BYPASSES_ARMOR)
                && player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.NOSSY_HAT.get())) {
            amount = Math.max(0F, amount - HatItem.DAMAGE_THRESHOLD);
        }
        return amount;
    }

    private static void plink(Player player) {
        PlayerState.plink(
                player,
                SoundEvents.ITEM_BREAK.value(),
                0.5F,
                1.0F + player.getRandom().nextFloat() * 0.5F);
    }

    private static boolean wearsAll(Player player, Item helmet, Item chest, Item legs, Item boots) {
        return is(player, EquipmentSlot.HEAD, helmet)
                && is(player, EquipmentSlot.CHEST, chest)
                && is(player, EquipmentSlot.LEGS, legs)
                && is(player, EquipmentSlot.FEET, boots);
    }

    private static boolean is(Player player, EquipmentSlot slot, Item item) {
        ItemStack stack = player.getItemBySlot(slot);
        return !stack.isEmpty() && stack.is(item);
    }
}
