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

package com.hbm.items.machine;

import com.hbm.items.ModDataComponents;
import com.hbm.util.NumberFormatter;
import com.hbm.util.TooltipStyle;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ZirnoxRodItem extends Item {

    public final EnumZirnoxType type;

    public ZirnoxRodItem(Properties props, EnumZirnoxType type) {
        super(props);
        this.type = type;
    }

    public static int getLifeTime(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ZIRNOX_LIFE.get(), 0);
    }

    public static void setLifeTime(ItemStack stack, int time) {
        stack.set(ModDataComponents.ZIRNOX_LIFE.get(), time);
    }

    public static void incrementLifeTime(ItemStack stack) {
        setLifeTime(stack, getLifeTime(stack) + 1);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getLifeTime(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        double frac = Math.min(1D, (double) getLifeTime(stack) / (double) type.maxLife);
        return (int) Math.round(13.0D * (1.0D - frac));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        double frac = Math.min(1D, (double) getLifeTime(stack) / (double) type.maxLife);
        return Mth.hsvToRgb((float) ((1.0D - frac) / 3.0D), 1.0F, 1.0F);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        double depletion =
                ((int) ((((double) getLifeTime(stack)) / (double) type.maxLife) * 100000)) / 1000D;
        adder.accept(
                Component.translatable("trait.rbmk.depletion", depletion + "%")
                        .withStyle(ChatFormatting.YELLOW));

        Component description =
                type.breeding
                        ? Component.translatable(
                                "desc.item.zirnoxBreedingRod",
                                NumberFormatter.formatCompact(type.maxLife))
                        : Component.translatable(
                                "desc.item.zirnoxRod",
                                type.heat,
                                NumberFormatter.formatCompact(type.maxLife));
        for (Component line : TooltipStyle.lines(description)) adder.accept(line);
    }

    public enum EnumZirnoxType {
        NATURAL_URANIUM_FUEL("natural_uranium_fuel", 250_000, 30, false, 0.35F),
        URANIUM_FUEL("uranium_fuel", 200_000, 50, false, 0.5F),
        TH232_FUEL("th232_fuel", 20_000, 0, true, 0.1F),
        THORIUM_FUEL("thorium_fuel", 200_000, 40, false, 1.75F),
        MOX_FUEL("mox_fuel", 165_000, 75, false, 2.5F),
        PLUTONIUM_FUEL("plutonium_fuel", 175_000, 65, false, 4.25F),
        U233_FUEL("u233_fuel", 150_000, 100, false, 5.0F),
        U235_FUEL("u235_fuel", 165_000, 85, false, 1.0F),
        LES_FUEL("les_fuel", 150_000, 150, false, 5.85F),
        LITHIUM_FUEL("lithium_fuel", 20_000, 0, true, 0.0F),
        ZFB_MOX_FUEL("zfb_mox_fuel", 50_000, 35, false, 2.5F);

        public static final EnumZirnoxType[] VALUES = values();

        public final String id;
        public final int maxLife;
        public final int heat;
        public final boolean breeding;
        public final float freshRad;

        EnumZirnoxType(String id, int life, int heat, boolean breeding, float freshRad) {
            this.id = id;
            maxLife = life;
            this.heat = heat;
            this.breeding = breeding;
            this.freshRad = freshRad;
        }

        public String legacyName() {
            return switch (this) {
                case TH232_FUEL -> "TH232";
                case LITHIUM_FUEL -> "LITHIUM";
                case ZFB_MOX_FUEL -> "ZFB_MOX";
                default -> name();
            };
        }
    }
}
