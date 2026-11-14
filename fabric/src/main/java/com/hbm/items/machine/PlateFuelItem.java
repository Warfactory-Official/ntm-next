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
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class PlateFuelItem extends Item {

    public final int lifeTime;
    public final FunctionEnum function;
    public final int reactivity;
    private final Supplier<DepletedFuelItem> spent;

    public PlateFuelItem(
            Properties properties,
            int lifeTime,
            FunctionEnum function,
            int reactivity,
            Supplier<DepletedFuelItem> spent) {
        super(properties);
        this.lifeTime = lifeTime;
        this.function = function;
        this.reactivity = reactivity;
        this.spent = spent;
    }

    public DepletedFuelItem spent() {
        return spent.get();
    }

    public static int getLifeTime(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.PLATE_FUEL_LIFE.get(), 0);
    }

    public static void setLifeTime(ItemStack stack, int time) {
        stack.set(ModDataComponents.PLATE_FUEL_LIFE.get(), time);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getLifeTime(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Mth.clamp(Math.round(13F - (float) getLifeTime(stack) * 13F / lifeTime), 0, 13);
    }

    public String getFunctionDesc() {
        return switch (function) {
            case LOGARITHM -> "f(x) = log10(x + 1) * 0.5 * " + reactivity;
            case SQUARE_ROOT -> "f(x) = sqrt(x) * " + reactivity + " / 10";
            case NEGATIVE_QUADRATIC -> "f(x) = [x - (x^2 / 10000)] / 100 * " + reactivity;
            case LINEAR -> "f(x) = x / 100 * " + reactivity;
            case PASSIVE -> "f(x) = " + reactivity;
        };
    }

    public int react(ItemStack stack, int flux) {
        if (function != FunctionEnum.PASSIVE) setLifeTime(stack, getLifeTime(stack) + flux);

        return switch (function) {
            case LOGARITHM -> (int) (Math.log10(flux + 1) * 0.5D * reactivity);
            case SQUARE_ROOT -> (int) (Math.sqrt(flux) * reactivity / 10D);
            case NEGATIVE_QUADRATIC ->
                    (int) Math.max((flux - (flux * (double) flux / 10000D)) / 100D * reactivity, 0);
            case LINEAR -> (int) (flux / 100D * reactivity);
            case PASSIVE -> {
                setLifeTime(stack, getLifeTime(stack) + reactivity);
                yield reactivity;
            }
        };
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(
                Component.translatable("desc.item.plateFuel.researchReactorPlateFuel")
                        .withStyle(ChatFormatting.YELLOW));
        adder.accept(
                Component.literal("   " + getFunctionDesc()).withStyle(ChatFormatting.DARK_AQUA));
        adder.accept(
                Component.translatable(
                                "desc.item.plateFuel.yieldOf",
                                NumberFormatter.formatCompact(lifeTime))
                        .withStyle(ChatFormatting.DARK_AQUA));
    }

    public enum FunctionEnum {
        LOGARITHM,
        SQUARE_ROOT,
        NEGATIVE_QUADRATIC,
        LINEAR,
        PASSIVE
    }
}
