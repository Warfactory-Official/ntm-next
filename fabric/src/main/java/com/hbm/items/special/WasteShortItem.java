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

package com.hbm.items.special;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class WasteShortItem extends Item {

    public final Kind type;

    public WasteShortItem(Properties props, Kind type) {
        super(props);
        this.type = type;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(Component.literal(type.label()).withStyle(ChatFormatting.ITALIC));
    }

    public interface Kind {
        String label();
    }

    public enum WasteClass implements Kind {
        URANIUM235("Uranium-235", 0, 100),
        URANIUM233("Uranium-233", 50, 100),
        NEPTUNIUM("Neptunium-237", 150, 500),
        PLUTONIUM239("Plutonium-239", 250, 1000),
        PLUTONIUM240("Plutonium-240", 350, 1000),
        PLUTONIUM241("Plutonium-241", 500, 1000),
        AMERICIUM242("Americium-242", 750, 1000),
        SCHRABIDIUM("Schrabidium-326", 1000, 1000);

        public static final WasteClass[] VALUES = values();

        public final String name;
        public final int liquid;
        public final int gas;

        WasteClass(String name, int liquid, int gas) {
            this.name = name;
            this.liquid = liquid;
            this.gas = gas;
        }

        @Override
        public String label() {
            return name;
        }
    }
}
