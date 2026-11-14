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

package com.hbm.items.machine;

import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

public class PACoilItem extends Item {

    public final EnumCoilType type;

    public PACoilItem(Properties properties, EnumCoilType type) {
        super(properties);
        this.type = type;
    }

    public static @Nullable EnumCoilType typeOf(ItemStack stack) {
        return stack.getItem() instanceof PACoilItem coil ? coil.type : null;
    }

    private static String num(int value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(
                Component.translatable(
                        "desc.item.paCoil.quadrupoleOperationalRange",
                        num(type.quadMin),
                        num(type.quadMax)));
        adder.accept(
                Component.translatable(
                        "desc.item.paCoil.dipoleOperationalRange",
                        num(type.diMin),
                        num(type.diMax)));
        adder.accept(
                Component.translatable("desc.item.paCoil.dipoleMinimumSideLength", type.diDistMin));
        adder.accept(
                Component.translatable("desc.item.paCoil.minimumsNotMetResult")
                        .withStyle(ChatFormatting.RED));
        adder.accept(
                Component.translatable("desc.item.paCoil.maximumsExceededResultIn")
                        .withStyle(ChatFormatting.RED));
        adder.accept(
                Component.translatable("desc.item.paCoil.particlesWillCrashIn")
                        .withStyle(ChatFormatting.RED));
    }

    public enum EnumCoilType {
        GOLD(0, 2_200, 0, 2_200, 15),
        NIOBIUM(1_500, 8_400, 1_500, 8_400, 21),
        BSCCO(7_500, 15_000, 7_500, 15_000, 27),
        CHLOROPHYTE(14_500, 75_000, 14_500, 75_000, 51);

        public static final EnumCoilType[] VALUES = values();

        public final int quadMin;
        public final int quadMax;
        public final int diMin;
        public final int diMax;
        public final int diDistMin;

        EnumCoilType(int quadMin, int quadMax, int diMin, int diMax, int diDistMin) {
            this.quadMin = quadMin;
            this.quadMax = quadMax;
            this.diMin = diMin;
            this.diMax = diMax;
            this.diDistMin = diDistMin;
        }
    }
}
