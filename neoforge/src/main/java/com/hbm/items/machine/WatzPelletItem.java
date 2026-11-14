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
import com.hbm.items.ModItems;
import com.hbm.util.ResponseFunction;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

import static com.hbm.util.ResponseFunction.fallingRoot;
import static com.hbm.util.ResponseFunction.linear;
import static com.hbm.util.ResponseFunction.quadratic;
import static com.hbm.util.ResponseFunction.root;

public class WatzPelletItem extends Item {
    public static final int SOURCE_LIGHT = 0xD2D2D2;
    public static final int SOURCE_DARK = 0x333333;
    public final boolean depleted;
    public final WatzFuel type;

    public WatzPelletItem(Properties properties, boolean depleted, WatzFuel type) {
        super(properties);
        this.depleted = depleted;
        this.type = type;
    }

    public static String spriteName(String item, WatzFuel type) {
        return item + "." + type.name().toLowerCase(Locale.ROOT);
    }

    public static double getYield(ItemStack stack) {
        Double stored = stack.get(ModDataComponents.WATZ_YIELD.get());
        return stored != null ? stored : typeOf(stack).yield();
    }

    public static void setYield(ItemStack stack, double yield) {
        stack.set(ModDataComponents.WATZ_YIELD.get(), yield);
    }

    public static double getEnrichment(ItemStack stack) {
        return getYield(stack) / typeOf(stack).yield();
    }

    public static WatzFuel typeOf(ItemStack stack) {
        return ((WatzPelletItem) stack.getItem()).type;
    }

    public static boolean isFresh(ItemStack stack) {
        return stack.getItem() instanceof WatzPelletItem pellet && !pellet.depleted;
    }

    public static boolean isDepleted(ItemStack stack) {
        return stack.getItem() instanceof WatzPelletItem pellet && pellet.depleted;
    }

    public static int desaturate(int color) {
        int r = (color & 0xff0000) >> 16;
        int g = (color & 0x00ff00) >> 8;
        int b = (color & 0x0000ff);

        int avg = (r + g + b) / 3;
        double approach = 0.9;
        double mult = 0.75;

        r -= (r - avg) * approach;
        g -= (g - avg) * approach;
        b -= (b - avg) * approach;

        r *= mult;
        g *= mult;
        b *= mult;

        return (r << 16) | (g << 8) | b;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return !depleted && getEnrichment(stack) < 1D;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return (int) Math.round(13.0D * Math.clamp(getEnrichment(stack), 0D, 1D));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x33FF33;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        if (depleted) return;

        WatzFuel num = typeOf(stack);

        adder.accept(
                Component.translatable(
                                "desc.shared.depletion",
                                String.format(
                                        Locale.ROOT, "%.1f", (1D - getEnrichment(stack)) * 100D))
                        .withStyle(ChatFormatting.GREEN));

        if (num.passive() > 0) {
            adder.accept(
                    Component.translatable("desc.item.watzPellet.baseFissionRate", num.passive()));
            adder.accept(
                    Component.translatable("desc.item.watzPellet.selfIgniting")
                            .withStyle(ChatFormatting.RED));
        }
        if (num.heatEmission() > 0)
            adder.accept(Component.translatable("desc.shared.heatPerFlux", num.heatEmission()));
        ResponseFunction burnFunc = num.burnFunc();
        if (burnFunc != null) {
            adder.accept(
                    Component.translatable("desc.shared.reactionFunction", burnFunc.formula()));
            adder.accept(Component.translatable("desc.shared.fuelType", burnFunc.danger()));
        }
        ResponseFunction heatDiv = num.heatDiv();
        if (heatDiv != null) {
            adder.accept(
                    Component.translatable(
                            "desc.item.watzPellet.thermalMultiplier", heatDiv.formula()));
        }
        ResponseFunction absorbFunc = num.absorbFunc();
        if (absorbFunc != null) {
            adder.accept(
                    Component.translatable(
                            "desc.item.watzPellet.fluxCapture", absorbFunc.formula()));
        }
    }

    public interface WatzFuel {
        String name();

        double yield();

        int colorLight();

        int colorDark();

        double passive();

        double heatEmission();

        double mudContent();

        @Nullable ResponseFunction burnFunc();

        @Nullable ResponseFunction heatDiv();

        @Nullable ResponseFunction absorbFunc();

        ItemStack depletedStack();
    }

    public enum EnumWatzType implements WatzFuel {
        SCHRABIDIUM(0x32FFFF, 0x005C5C, 2_000, 20D, 0.01D, linear(1.5D), fallingRoot(10D), null),
        HES(0x66DCD6, 0x023933, 1_750, 20D, 0.005D, linear(1.25D), fallingRoot(15D), null),
        MES(0xCBEADF, 0x28473C, 1_500, 15D, 0.0025D, linear(1.15D), fallingRoot(15D), null),
        LES(0xABB4A8, 0x0C1105, 1_250, 15D, 0.00125D, linear(1D), fallingRoot(20D), null),
        HEN(0xA6B2A6, 0x030F03, 0, 10D, 0.0005D, root(100), fallingRoot(10D), null),
        MEU(0xC1C7BD, 0x2B3227, 0, 10D, 0.0005D, root(75), fallingRoot(10D), null),
        MEP(0x9AA3A0, 0x111A17, 0, 15D, 0.0005D, root(150), fallingRoot(10D), null),
        LEAD(0xA6A6B2, 0x03030F, 0, 0, 0.0025D, null, null, root(10)),
        BORON(0xBDC8D2, 0x29343E, 0, 0, 0.0025D, null, null, linear(10)),
        DU(0xC1C7BD, 0x2B3227, 0, 0, 0.0025D, null, null, quadratic(1D, 1D).withDivisor(100)),
        NQD(
                0x4B4B4B,
                0x121212,
                2_000,
                20,
                0.01D,
                linear(2D),
                root(1D / 25D).withOffset(25D * 25D),
                null),
        NQR(
                0x2D2D2D,
                0x0B0B0B,
                2_500,
                30,
                0.01D,
                linear(1.5D),
                root(1D / 25D).withOffset(25D * 25D),
                null);

        public final double yield = 500_000_000;
        public final int colorLight;
        public final int colorDark;
        public final double mudContent;
        public final double passive;
        public final double heatEmission;
        public final @Nullable ResponseFunction burnFunc;
        public final @Nullable ResponseFunction heatDiv;
        public final @Nullable ResponseFunction absorbFunc;

        EnumWatzType(
                int colorLight,
                int colorDark,
                double passive,
                double heatEmission,
                double mudContent,
                @Nullable ResponseFunction burnFunction,
                @Nullable ResponseFunction heatDivisor,
                @Nullable ResponseFunction absorbFunction) {
            this.colorLight = colorLight;
            this.colorDark = colorDark;
            this.passive = passive;
            this.heatEmission = heatEmission;
            this.mudContent = mudContent / 2D;
            this.burnFunc = burnFunction;
            this.heatDiv = heatDivisor;
            this.absorbFunc = absorbFunction;
        }

        @Override
        public double yield() {
            return yield;
        }

        @Override
        public int colorLight() {
            return colorLight;
        }

        @Override
        public int colorDark() {
            return colorDark;
        }

        @Override
        public double passive() {
            return passive;
        }

        @Override
        public double heatEmission() {
            return heatEmission;
        }

        @Override
        public double mudContent() {
            return mudContent;
        }

        @Override
        public @Nullable ResponseFunction burnFunc() {
            return burnFunc;
        }

        @Override
        public @Nullable ResponseFunction heatDiv() {
            return heatDiv;
        }

        @Override
        public @Nullable ResponseFunction absorbFunc() {
            return absorbFunc;
        }

        @Override
        public ItemStack depletedStack() {
            return ModItems.WATZ_PELLET_DEPLETED.stack(this);
        }
    }
}
