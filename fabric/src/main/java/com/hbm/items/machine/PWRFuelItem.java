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

import com.hbm.util.ResponseFunction;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import static com.hbm.util.ResponseFunction.logarithmic;
import static com.hbm.util.ResponseFunction.root;

public class PWRFuelItem extends Item {

    public final EnumPWRFuel fuel;

    public PWRFuelItem(Properties props, EnumPWRFuel fuel) {
        super(props);
        this.fuel = fuel;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(Component.translatable("desc.shared.heatPerFlux", fuel.heatEmission));
        adder.accept(
                Component.translatable("desc.shared.reactionFunction", fuel.function.formula()));
        adder.accept(Component.translatable("desc.shared.fuelType", fuel.function.danger()));
    }

    public enum EnumPWRFuel {
        MEU("meu", 5.0D, logarithmic(20 * 30).withDivisor(2_500)),
        HEU233("heu233", 7.5D, root(25)),
        HEU235("heu235", 7.5D, root(22.5)),
        MEN("men", 7.5D, logarithmic(22.5 * 30).withDivisor(2_500)),
        HEN237("hen237", 7.5D, root(27.5)),
        MOX("mox", 7.5D, logarithmic(20 * 30).withDivisor(2_500)),
        MEP("mep", 7.5D, logarithmic(22.5 * 30).withDivisor(2_500)),
        HEP239("hep239", 10.0D, root(22.5)),
        HEP241("hep241", 10.0D, root(25)),
        MEA("mea", 7.5D, logarithmic(25 * 30).withDivisor(2_500)),
        HEA242("hea242", 10.0D, root(25)),
        HES326("hes326", 12.5D, root(27.5)),
        HES327("hes327", 12.5D, root(30)),
        BFB_AM_MIX("bfb_am_mix", 2.5D, root(15), 250_000_000),
        BFB_PU241("bfb_pu241", 2.5D, root(15), 250_000_000);

        public static final EnumPWRFuel[] VALUES = values();

        public final String id;
        public final double yield;
        public final double heatEmission;
        public final ResponseFunction function;

        EnumPWRFuel(String id, double heatEmission, ResponseFunction function, double yield) {
            this.id = id;
            this.heatEmission = heatEmission;
            this.function = function;
            this.yield = yield;
        }

        EnumPWRFuel(String id, double heatEmission, ResponseFunction function) {
            this(id, heatEmission, function, 1_000_000_000);
        }
    }
}
