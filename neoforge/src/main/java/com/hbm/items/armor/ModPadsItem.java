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

import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.ArmorUtil;
import com.hbm.items.ModItems;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ModPadsItem extends ArmorModItem {

    private final float damageMod;

    public ModPadsItem(Properties properties, float damageMod) {
        super(properties, ArmorModHandler.BOOTS_ONLY, false, false, false, true);
        this.damageMod = damageMod;
    }

    private int fallPercent() {
        return Math.round((1F - damageMod) * 100F);
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        if (damageMod != 1F)
            adder.accept(
                    Component.translatable("desc.item.armorMod.pads", fallPercent())
                            .withStyle(ChatFormatting.RED));
        if (this == ModItems.PADS_STATIC.get())
            adder.accept(
                    Component.translatable("desc.item.armorMod.pads.static")
                            .withStyle(ChatFormatting.DARK_PURPLE));
    }

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        String key =
                this == ModItems.PADS_STATIC.get()
                        ? "desc.item.armorMod.pads.static.installed"
                        : "desc.item.armorMod.pads.installed";
        tooltip.add(
                Component.translatable(key, stack.getHoverName(), fallPercent())
                        .withStyle(ChatFormatting.DARK_PURPLE));
    }

    @Override
    public float modDamage(
            LivingEntity entity, DamageSource source, float amount, ItemStack armor) {
        return source.is(DamageTypes.FALL) ? amount * damageMod : amount;
    }

    @Override
    public boolean ticksServer() {
        return this == ModItems.PADS_STATIC.get();
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (entity.level().isClientSide()
                || this != ModItems.PADS_STATIC.get()
                || !(entity instanceof Player player)) return;
        if (!player.walkAnimation.isMoving()) return;

        if (!(player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ModArmorItem chest))
            return;
        if (!ArmorSuitEffects.hasFullSet(player, chest.suit(), false)) return;

        ArmorFullSetBonus bonus = ArmorFullSetBonus.get(chest.suit());
        long drain = bonus == null ? 0L : bonus.drain();

        for (EquipmentSlot slot : ArmorUtil.ARMOR_SLOTS) {
            ItemStack piece = player.getItemBySlot(slot);
            if (!(piece.getItem() instanceof ModArmorItem powered) || !powered.isPowered())
                continue;
            long charge = drain / 2L;
            if (charge == 0L) charge = powered.consumption() / 40L;
            powered.setCharge(piece, powered.getCharge(piece) + charge);
        }
    }
}
