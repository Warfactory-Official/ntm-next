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

import com.hbm.items.ModDataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ArcElectrodeItem extends Item {

    public final EnumElectrodeType type;

    public ArcElectrodeItem(Properties props, EnumElectrodeType type) {
        super(props);
        this.type = type;
    }

    public static int getDurability(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ARC_ELECTRODE_DURABILITY.get(), 0);
    }

    public static boolean damage(ItemStack stack) {
        int durability = getDurability(stack) + 1;
        stack.set(ModDataComponents.ARC_ELECTRODE_DURABILITY.get(), durability);
        return durability >= getMaxDurability(stack);
    }

    public static int getMaxDurability(ItemStack stack) {
        return ((ArcElectrodeItem) stack.getItem()).type.durability;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getDurability(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        double frac =
                Math.min(1D, (double) getDurability(stack) / (double) getMaxDurability(stack));
        return (int) Math.round(13.0D * (1.0D - frac));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        double frac =
                Math.min(1D, (double) getDurability(stack) / (double) getMaxDurability(stack));
        return Mth.hsvToRgb((float) ((1.0D - frac) / 3.0D), 1.0F, 1.0F);
    }

    public enum EnumElectrodeType {
        GRAPHITE(10),
        LANTHANIUM(100),
        DESH(500),
        SATURNITE(1500);

        public static final EnumElectrodeType[] VALUES = values();

        public final int durability;

        EnumElectrodeType(int dura) {
            this.durability = dura;
        }
    }
}
