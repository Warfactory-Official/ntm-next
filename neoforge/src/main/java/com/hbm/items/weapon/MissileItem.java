/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

package com.hbm.items.weapon;

import com.hbm.items.special.CustomLoreItem;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class MissileItem extends CustomLoreItem {

    public final MissileFormFactor formFactor;
    public final MissileTier tier;
    public final MissileFuel fuel;
    public final int fuelCap;
    public final boolean launchable;

    public MissileItem(Properties props, MissileFormFactor form, MissileTier tier) {
        this(props, form, tier, form.defaultFuel, true);
    }

    public MissileItem(
            Properties props,
            MissileFormFactor form,
            MissileTier tier,
            MissileFuel fuel,
            boolean launchable) {
        super(props);
        this.formFactor = form;
        this.tier = tier;
        this.fuel = fuel;
        this.fuelCap = fuel.defaultCap;
        this.launchable = launchable;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(
                Component.translatable(
                                "item.hbm.missile.tier." + tier.name().toLowerCase(Locale.ROOT))
                        .withStyle(ChatFormatting.ITALIC));
        if (!launchable) {
            adder.accept(
                    Component.translatable("item.hbm.missile.desc.notLaunchable")
                            .withStyle(ChatFormatting.RED));
            return;
        }
        adder.accept(
                Component.translatable("item.hbm.missile.desc.fuel")
                        .append(": ")
                        .append(fuel.getDisplay()));
        if (fuelCap > 0) {
            adder.accept(
                    Component.translatable("item.hbm.missile.desc.fuelCapacity")
                            .append(": " + fuelCap + "mB"));
        }
        super.appendHoverText(stack, context, display, adder, flag);
    }

    public enum MissileFormFactor {
        ABM(MissileFuel.SOLID),
        MICRO(MissileFuel.SOLID),
        V2(MissileFuel.ETHANOL_PEROXIDE),
        STRONG(MissileFuel.KEROSENE_PEROXIDE),
        HUGE(MissileFuel.KEROSENE_LOXY),
        ATLAS(MissileFuel.JETFUEL_LOXY),
        OTHER(MissileFuel.KEROSENE_PEROXIDE);

        public final MissileFuel defaultFuel;

        MissileFormFactor(MissileFuel defaultFuel) {
            this.defaultFuel = defaultFuel;
        }
    }

    public enum MissileTier {
        TIER0,
        TIER1,
        TIER2,
        TIER3,
        TIER4
    }

    public enum MissileFuel {
        SOLID("item.hbm.missile.fuel.solid.prefueled", ChatFormatting.GOLD, 0),
        ETHANOL_PEROXIDE("item.hbm.missile.fuel.ethanol_peroxide", ChatFormatting.AQUA, 4_000),
        KEROSENE_PEROXIDE("item.hbm.missile.fuel.kerosene_peroxide", ChatFormatting.BLUE, 8_000),
        KEROSENE_LOXY("item.hbm.missile.fuel.kerosene_loxy", ChatFormatting.LIGHT_PURPLE, 12_000),
        JETFUEL_LOXY("item.hbm.missile.fuel.jetfuel_loxy", ChatFormatting.RED, 16_000);

        public final ChatFormatting color;
        public final int defaultCap;
        private final String key;

        MissileFuel(String key, ChatFormatting color, int defaultCap) {
            this.key = key;
            this.color = color;
            this.defaultCap = defaultCap;
        }

        public Component getDisplay() {
            return Component.translatable(key).withStyle(color);
        }
    }
}
