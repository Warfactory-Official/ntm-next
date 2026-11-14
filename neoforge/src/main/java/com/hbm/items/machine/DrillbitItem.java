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

public class DrillbitItem extends Item {

    public final EnumDrillType type;

    public DrillbitItem(Properties properties, EnumDrillType type) {
        super(properties);
        this.type = type;
    }

    public static @Nullable EnumDrillType typeOf(ItemStack stack) {
        return stack.getItem() instanceof DrillbitItem bit ? bit.type : null;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(
                Component.translatable("desc.shared.speed", (int) (type.speed * 100) + "%")
                        .withStyle(ChatFormatting.YELLOW));
        adder.accept(
                Component.translatable("desc.item.drillbit.tier", type.tier)
                        .withStyle(ChatFormatting.YELLOW));
        if (type.fortune > 0) {
            adder.accept(
                    Component.translatable("desc.item.drillbit.fortune", type.fortune)
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (type.vein)
            adder.accept(
                    Component.translatable("desc.item.drillbit.veinMiner")
                            .withStyle(ChatFormatting.GREEN));
        if (type.silk)
            adder.accept(
                    Component.translatable("desc.item.drillbit.silkTouch")
                            .withStyle(ChatFormatting.GREEN));
    }

    public enum EnumDrillType {
        STEEL(1.0D, 1, 0, false, false),
        STEEL_DIAMOND(1.0D, 1, 2, false, true),
        HSS(1.2D, 2, 0, true, false),
        HSS_DIAMOND(1.2D, 2, 3, true, true),
        DESH(1.5D, 3, 1, true, true),
        DESH_DIAMOND(1.5D, 3, 4, true, true),
        TCALLOY(2.0D, 4, 1, true, true),
        TCALLOY_DIAMOND(2.0D, 4, 4, true, true),
        FERRO(2.5D, 5, 1, true, true),
        FERRO_DIAMOND(2.5D, 5, 4, true, true);

        public static final EnumDrillType[] VALUES = values();
        public final double speed;
        public final int tier;
        public final int fortune;
        public final boolean vein;
        public final boolean silk;
        public final String id = name().toLowerCase(Locale.ROOT);

        EnumDrillType(double speed, int tier, int fortune, boolean vein, boolean silk) {
            this.speed = speed;
            this.tier = tier;
            this.fortune = fortune;
            this.vein = vein;
            this.silk = silk;
        }
    }
}
