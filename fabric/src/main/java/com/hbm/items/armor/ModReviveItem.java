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
import com.hbm.items.ModItems;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ModReviveItem extends ArmorModItem {
    public ModReviveItem(Properties properties, int durability) {
        super(properties.durability(durability), ArmorModHandler.EXTRA, false, false, true, false);
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        if (this == ModItems.SCRUMPY.get()) {
            adder.accept(
                    Component.translatable("desc.item.modRevive.butHowDidYou")
                            .withStyle(ChatFormatting.GOLD));
            adder.accept(
                    Component.translatable("desc.item.modRevive.iWasDrunk")
                            .withStyle(ChatFormatting.RED));
        }
        if (this == ModItems.WILD_P.get()) {
            adder.accept(
                    Component.translatable("desc.item.modRevive.explosive")
                            .withStyle(ChatFormatting.DARK_GRAY)
                            .append(
                                    Component.translatable("desc.item.modRevive.reactive")
                                            .withStyle(ChatFormatting.RED))
                            .append(
                                    Component.translatable("desc.item.modRevive.plot")
                                            .withStyle(ChatFormatting.DARK_GRAY))
                            .append(
                                    Component.translatable("desc.item.modRevive.armor")
                                            .withStyle(ChatFormatting.RED)));
        }
        adder.accept(Component.empty());
        adder.accept(
                Component.literal((stack.getMaxDamage() - stack.getDamageValue()) + " revives left")
                        .withStyle(ChatFormatting.GOLD));
    }

    @Override
    protected boolean hasTooltipSpacer() {
        return false;
    }

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(
                Component.literal("  ")
                        .append(stack.getHoverName())
                        .append(
                                " ("
                                        + (stack.getMaxDamage() - stack.getDamageValue())
                                        + " revives left)")
                        .withStyle(ChatFormatting.GOLD));
    }
}
