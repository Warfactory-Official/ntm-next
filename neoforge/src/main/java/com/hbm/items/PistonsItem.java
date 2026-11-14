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

package com.hbm.items;

import com.hbm.inventory.fluid.trait.CombustibleTrait;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class PistonsItem extends Item {

    public final EnumPistonType type;

    public PistonsItem(Properties properties, EnumPistonType type) {
        super(properties);
        this.type = type;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(efficiencyHeader());
        var grades = CombustibleTrait.Grade.values();
        for (int i = 0; i < type.eff.length; i++)
            adder.accept(efficiencyLine(grades[i], type.eff[i]));
    }

    public static Component efficiencyHeader() {
        return Component.translatable("item.hbm.piston_set.eff").withStyle(ChatFormatting.YELLOW);
    }

    public static Component efficiencyLine(CombustibleTrait.Grade grade, double efficiency) {
        return Component.literal("-")
                .append(Component.translatable(grade.labelKey()))
                .append(": ")
                .withStyle(ChatFormatting.YELLOW)
                .append(
                        Component.literal((int) (efficiency * 100) + "%")
                                .withStyle(ChatFormatting.RED));
    }

    public enum EnumPistonType {
        STEEL(1.00, 0.75, 0.25, 0.00, 0.00),
        DURA(0.50, 1.00, 0.90, 0.50, 0.00),
        DESH(0.00, 0.50, 1.00, 0.75, 0.00),
        STARMETAL(0.50, 0.75, 1.00, 0.90, 0.50);

        public static final EnumPistonType[] VALUES = values();
        public final double[] eff;
        public final String id = name().toLowerCase(Locale.ROOT);

        EnumPistonType(double... eff) {
            this.eff = new double[Math.min(CombustibleTrait.Grade.values().length, eff.length)];
            System.arraycopy(eff, 0, this.eff, 0, this.eff.length);
        }
    }
}
