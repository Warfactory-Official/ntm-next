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

package com.hbm.items.weapon.sedna.impl;

import com.hbm.items.ItemCustomizationHandler;
import com.hbm.items.weapon.sedna.GunBaseNTItem;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.GunTimers;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

public class GunNI4NIItem extends GunBaseNTItem implements ItemCustomizationHandler {

    public static final String KEY_COIN_COUNT = "coincount";
    public static final String KEY_COIN_CHARGE = "coincharge";

    public GunNI4NIItem(
            WeaponQuality quality, Item.Properties properties, GunConfig.Builder... cfg) {
        super(quality, properties, cfg);
    }

    public static void resetColors(ItemStack stack) {
        removeValue(stack, "colors");
    }

    public static void setColors(ItemStack stack, int dark, int light, int grip) {
        setValueIntArray(stack, "colors", new int[] {dark, light, grip});
    }

    public static int[] getColors(ItemStack stack) {
        int[] colors = getValueIntArray(stack, "colors");
        return colors.length == 3 ? colors : null;
    }

    public static int getCoinCount(ItemStack stack) {
        return getValueInt(stack, KEY_COIN_COUNT);
    }

    public static void setCoinCount(ItemStack stack, int value) {
        setValueInt(stack, KEY_COIN_COUNT, value);
    }

    public static int getCoinCharge(ItemStack stack) {
        GunTimers entry = GunTimers.of(stack);
        return entry != null ? entry.coinCharge() : getValueInt(stack, KEY_COIN_CHARGE);
    }

    public static void setCoinCharge(ItemStack stack, int value) {
        GunTimers.entry(stack).setCoinCharge(value);
    }

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);

        int maxCoin = 4;
        if (XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_NI4NI_NICKEL)) maxCoin += 2;
        if (XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_NI4NI_DOUBLOONS))
            maxCoin += 2;

        int count = getCoinCount(stack);
        if (count >= maxCoin) return;

        var timers = GunTimers.entry(stack);
        int charge = timers.coinCharge() + 1;
        if (charge < 80) {
            timers.setCoinCharge(charge);
            return;
        }
        timers.setCoinCharge(0);
        setCoinCount(stack, ++count);
        if (slot == EquipmentSlot.MAINHAND) {
            EntitySounds.play(
                    entity,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    ModSounds.TECH_BOOP.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1F + count / (float) maxCoin);
        }
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        tooltip.accept(Component.translatable("desc.item.gunNI4NI.nowDonTGet"));
        tooltip.accept(
                Component.translatable("desc.item.gunNI4NI.i")
                        .withStyle(ChatFormatting.GRAY)
                        .append(
                                Component.translatable("desc.item.gunNI4NI.fuckingHate")
                                        .withStyle(ChatFormatting.RED))
                        .append(
                                Component.translatable("desc.item.gunNI4NI.thisGame")
                                        .withStyle(ChatFormatting.GRAY)));
        tooltip.accept(Component.translatable("desc.item.gunNI4NI.iDidnTDo"));
        super.appendHoverText(stack, context, display, tooltip, flag);
    }

    @Override
    public void customizeByCommand(ServerPlayer player, ItemStack stack, List<String> words) {
        if (words.isEmpty()) {
            resetColors(stack);
            player.sendSystemMessage(
                    Component.translatable("desc.item.gunNI4NI.colorsReset")
                            .withStyle(ChatFormatting.GREEN));
            return;
        }
        if (words.size() != 3) {
            resetColors(stack);
            player.sendSystemMessage(
                    Component.translatable("desc.item.gunNI4NI.requiresThreeHexadecimal")
                            .withStyle(ChatFormatting.RED));
            return;
        }
        int dark;
        int light;
        int grip;
        try {
            dark = Integer.parseInt(words.get(0), 16);
            light = Integer.parseInt(words.get(1), 16);
            grip = Integer.parseInt(words.get(2), 16);
        } catch (NumberFormatException ex) {
            player.sendSystemMessage(
                    Component.literal(ex.getLocalizedMessage()).withStyle(ChatFormatting.RED));
            return;
        }
        if ((dark | light | grip) >>> 24 != 0) {
            player.sendSystemMessage(
                    Component.translatable("desc.item.gunNI4NI.colorsMustRangeFrom")
                            .withStyle(ChatFormatting.RED));
            return;
        }
        setColors(stack, dark, light, grip);
        player.sendSystemMessage(
                Component.translatable("desc.item.gunNI4NI.colorsSet")
                        .withStyle(ChatFormatting.GREEN));
    }
}
