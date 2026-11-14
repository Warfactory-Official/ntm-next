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

import com.hbm.blockentity.machine.rbmk.NeutronSpeed;
import com.hbm.data.MachineData;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.machine.RBMKFuelData;
import com.hbm.items.machine.RBMKPelletItem;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class RBMKRodItem extends Item {
    public @Nullable RBMKPelletItem pellet;

    public String fullName = "";
    public double reactivity;
    public double selfRate;
    public BurnFunction function = EnumBurnFunc.LOG_TEN;
    public DepletionFunction depFunc = EnumDepleteFunc.GENTLE_SLOPE;
    public double xGen = 0.5D;
    public double xBurn = 50.0D;
    public double heat = 1.0D;
    public double yield;
    public double meltingPoint = 1000.0D;
    public double diffusion = 0.02D;
    public NeutronSpeed nType = NeutronSpeed.SLOW;
    public NeutronSpeed rType = NeutronSpeed.FAST;
    public int colorTint = 0x304825;
    public double heatCoeffStart = 0.0D;
    public double heatCoeffLength = 0.0D;

    public RBMKRodItem(Properties props, String fullName) {
        super(props);
        this.fullName = fullName;
    }

    public static double getEnrichment(ItemStack stack) {
        return getYield(stack) / ((RBMKRodItem) stack.getItem()).yield;
    }

    public static double getPoisonLevel(ItemStack stack) {
        return getPoison(stack) / 100.0D;
    }

    public static RBMKFuelData getData(ItemStack stack) {
        RBMKFuelData d = stack.get(ModDataComponents.RBMK_FUEL.get());
        if (d != null) return d;
        return RBMKFuelData.fresh(stack.getItem() instanceof RBMKRodItem rod ? rod.yield : 0);
    }

    public static double getYield(ItemStack stack) {
        return getData(stack).yield();
    }

    public static void setYield(ItemStack stack, double v) {
        stack.set(ModDataComponents.RBMK_FUEL.get(), getData(stack).withYield(v));
    }

    public static double getPoison(ItemStack stack) {
        return getData(stack).xenon();
    }

    public static void setPoison(ItemStack stack, double v) {
        stack.set(ModDataComponents.RBMK_FUEL.get(), getData(stack).withXenon(v));
    }

    public static double getCoreHeat(ItemStack stack) {
        return getData(stack).coreHeat();
    }

    public static void setCoreHeat(ItemStack stack, double v) {
        stack.set(ModDataComponents.RBMK_FUEL.get(), getData(stack).withCoreHeat(v));
    }

    public static double getHullHeat(ItemStack stack) {
        return getData(stack).hullHeat();
    }

    public static void setHullHeat(ItemStack stack, double v) {
        stack.set(ModDataComponents.RBMK_FUEL.get(), getData(stack).withHullHeat(v));
    }

    public RBMKRodItem setTint(int tint) {
        colorTint = tint;
        return this;
    }

    public RBMKRodItem setYield(double yield) {
        this.yield = yield;
        return this;
    }

    public RBMKRodItem setStats(double funcEnd) {
        return setStats(funcEnd, 0);
    }

    public RBMKRodItem setStats(double funcEnd, double selfRate) {
        reactivity = funcEnd;
        this.selfRate = selfRate;
        return this;
    }

    public RBMKRodItem setFunction(BurnFunction func) {
        function = func;
        return this;
    }

    public RBMKRodItem setDepletionFunction(DepletionFunction func) {
        depFunc = func;
        return this;
    }

    public RBMKRodItem setHeatCoeff(double start, double length) {
        heatCoeffStart = start;
        heatCoeffLength = length;
        return this;
    }

    public RBMKRodItem setXenon(double gen, double burn) {
        xGen = gen;
        xBurn = burn;
        return this;
    }

    public RBMKRodItem setHeat(double heat) {
        this.heat = heat;
        return this;
    }

    public RBMKRodItem setDiffusion(double diffusion) {
        this.diffusion = diffusion;
        return this;
    }

    public RBMKRodItem setMeltingPoint(double meltingPoint) {
        this.meltingPoint = meltingPoint;
        return this;
    }

    public RBMKRodItem setNeutronTypes(NeutronSpeed nType, NeutronSpeed rType) {
        this.nType = nType;
        this.rType = rType;
        return this;
    }

    public double burn(Level world, ItemStack stack, double inFlux) {

        inFlux += selfRate;

        if (!MachineData.RBMK_DISABLE_XENON.get()) {
            double xenon = getPoison(stack);
            xenon -= xenonBurnFunc(inFlux);
            inFlux *= (1.0D - getPoisonLevel(stack));
            xenon += xenonGenFunc(inFlux);
            if (xenon < 0.0D) xenon = 0.0D;
            if (xenon > 100.0D) xenon = 100.0D;
            setPoison(stack, xenon);
        }

        double mult = 1.0D;
        double coreHeat = getCoreHeat(stack);

        if (heatCoeffStart != 0 && coreHeat >= heatCoeffStart) {
            double prog = (coreHeat - heatCoeffStart) / heatCoeffLength;
            if (prog > 1) prog = 1;
            mult = Math.sin((prog * Math.PI + Math.PI) / 2);
        }

        double outFlux =
                reactivityFunc(inFlux, getEnrichment(stack) * mult)
                        * MachineData.RBMK_REACTIVITY_MOD.get();

        if (!MachineData.RBMK_DISABLE_DEPLETION.get()) {
            double y = getYield(stack);
            y -= inFlux;
            if (y < 0.0D) y = 0.0D;
            setYield(stack, y);
        }
        coreHeat += outFlux * heat;
        setCoreHeat(stack, rectify(coreHeat));

        return outFlux;
    }

    private double rectify(double num) {
        if (num > 1_000_000.0D) num = 1_000_000.0D;
        if (num < 20.0D || Double.isNaN(num)) num = 20.0D;
        return num;
    }

    public void updateHeat(Level world, ItemStack stack, double mod) {
        double coreHeat = getCoreHeat(stack);
        double hullHeat = getHullHeat(stack);
        if (coreHeat > hullHeat) {
            double mid = (coreHeat - hullHeat) / 2.0D;
            coreHeat -= mid * diffusion * MachineData.RBMK_FUEL_DIFFUSION_MOD.get() * mod;
            hullHeat += mid * diffusion * MachineData.RBMK_FUEL_DIFFUSION_MOD.get() * mod;
            setCoreHeat(stack, rectify(coreHeat));
            setHullHeat(stack, rectify(hullHeat));
        }
    }

    public double provideHeat(Level world, ItemStack stack, double heat, double mod) {
        double hullHeat = getHullHeat(stack);

        if (hullHeat > meltingPoint) {
            double coreHeat = getCoreHeat(stack);
            double avg = (heat + hullHeat + coreHeat) / 3.0D;
            setCoreHeat(stack, avg);
            setHullHeat(stack, avg);
            return avg - heat;
        }

        if (hullHeat <= heat) return 0;

        double ret = (hullHeat - heat) / 2;
        ret *= MachineData.RBMK_HEAT_PROVISION.get() * mod;
        hullHeat -= ret;
        setHullHeat(stack, hullHeat);
        return ret;
    }

    public double reactivityFunc(double in, double enrichment) {
        double flux = in * reactivityModByEnrichment(enrichment);
        return function.reactivity(this, flux, enrichment);
    }

    public double reactivityModByEnrichment(double enrichment) {
        return depFunc.modify(enrichment);
    }

    public double xenonGenFunc(double flux) {
        return flux * xGen;
    }

    public double xenonBurnFunc(double flux) {
        return (flux * flux) / xBurn;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getEnrichment(stack) < 1.0D;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return (int) Math.round(13.0D * Math.clamp(getEnrichment(stack), 0.0D, 1.0D));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb(
                (float) Math.clamp(getEnrichment(stack), 0.0D, 1.0D) / 3.0F, 1.0F, 1.0F);
    }

    public String getFuncDescription(ItemStack stack) {
        String function = this.function.formula(this);

        double enrichment = getEnrichment(stack);
        String flux =
                selfRate > 0
                        ? "(x" + ChatFormatting.RED + " + " + selfRate + ChatFormatting.WHITE + ")"
                        : "x";

        if (enrichment < 1) {
            enrichment = reactivityModByEnrichment(enrichment);
            String enrichmentMod =
                    ChatFormatting.YELLOW
                            + ""
                            + ((int) (enrichment * 1000.0D) / 1000.0D)
                            + ChatFormatting.WHITE;
            String enrichmentPer =
                    ChatFormatting.GOLD + " (" + ((int) (enrichment * 1000.0D) / 10.0D) + "%)";
            flux = "(" + flux + " * " + enrichmentMod + ")";
            return String.format(Locale.ROOT, function, flux, reactivity).concat(enrichmentPer);
        }

        return String.format(Locale.ROOT, function, flux, reactivity);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(Component.literal(fullName).withStyle(ChatFormatting.ITALIC));

        if (this == ModItems.RBMK_FUEL_DRX.get()) {
            if (getHullHeat(stack) >= 50 || getCoreHeat(stack) >= 50) {
                adder.accept(
                        Component.translatable("desc.item.wasteCooling")
                                .withStyle(ChatFormatting.GOLD));
            }
            if (selfRate > 0 || function == EnumBurnFunc.SIGMOID) {
                adder.accept(
                        Component.translatable("trait.rbmx.source").withStyle(ChatFormatting.RED));
            }
            adder.accept(
                    Component.translatable(
                                    "trait.rbmx.depletion",
                                    ((int) (((yield - getYield(stack)) / yield) * 100000)) / 1000.0D
                                            + "%")
                            .withStyle(ChatFormatting.GREEN));
            adder.accept(
                    Component.translatable(
                                    "trait.rbmx.xenon",
                                    ((int) (getPoison(stack) * 1000.0D) / 1000.0D) + "%")
                            .withStyle(ChatFormatting.DARK_PURPLE));
            adder.accept(
                    Component.translatable(
                                    "trait.rbmx.splitsWith",
                                    Component.translatable(nType.rbmxKey()))
                            .withStyle(ChatFormatting.BLUE));
            adder.accept(
                    Component.translatable(
                                    "trait.rbmx.splitsInto",
                                    Component.translatable(rType.rbmxKey()))
                            .withStyle(ChatFormatting.BLUE));
            adder.accept(
                    Component.translatable(
                                    "trait.rbmx.fluxFunc",
                                    ChatFormatting.WHITE + getFuncDescription(stack))
                            .withStyle(ChatFormatting.YELLOW));
            adder.accept(
                    Component.translatable(
                                    "trait.rbmx.funcType", Component.translatable(function.title()))
                            .withStyle(ChatFormatting.YELLOW));
            adder.accept(
                    Component.translatable(
                                    "trait.rbmx.xenonGen", ChatFormatting.WHITE + "x * " + xGen)
                            .withStyle(ChatFormatting.YELLOW));
            adder.accept(
                    Component.translatable(
                                    "trait.rbmx.xenonBurn", ChatFormatting.WHITE + "x² / " + xBurn)
                            .withStyle(ChatFormatting.YELLOW));
            adder.accept(
                    Component.translatable("trait.rbmx.heat", heat + "°C")
                            .withStyle(ChatFormatting.GOLD));
            adder.accept(
                    Component.translatable("trait.rbmx.diffusion", diffusion + "¹/²")
                            .withStyle(ChatFormatting.GOLD));
            adder.accept(
                    Component.translatable(
                                    "trait.rbmx.skinTemp",
                                    ((int) (getHullHeat(stack) * 10.0D) / 10.0D) + "m")
                            .withStyle(ChatFormatting.RED));
            adder.accept(
                    Component.translatable(
                                    "trait.rbmx.coreTemp",
                                    ((int) (getCoreHeat(stack) * 10.0D) / 10.0D) + "m")
                            .withStyle(ChatFormatting.RED));
            adder.accept(
                    Component.translatable("trait.rbmx.melt", meltingPoint + "m")
                            .withStyle(ChatFormatting.DARK_RED));
        } else {
            if (getHullHeat(stack) >= 50 || getCoreHeat(stack) >= 50) {
                adder.accept(
                        Component.translatable("desc.item.wasteCooling")
                                .withStyle(ChatFormatting.GOLD));
            }
            if (selfRate > 0 || function == EnumBurnFunc.SIGMOID) {
                adder.accept(
                        Component.translatable("trait.rbmk.source").withStyle(ChatFormatting.RED));
            }
            adder.accept(
                    Component.translatable(
                                    "trait.rbmk.depletion",
                                    ((int) (((yield - getYield(stack)) / yield) * 100000.0D))
                                                    / 1000.0D
                                            + "%")
                            .withStyle(ChatFormatting.GREEN));
            adder.accept(
                    Component.translatable(
                                    "trait.rbmk.xenon",
                                    ((int) (getPoison(stack) * 1000.0D) / 1000.0D) + "%")
                            .withStyle(ChatFormatting.DARK_PURPLE));
            adder.accept(
                    Component.translatable(
                                    "trait.rbmk.splitsWith", Component.translatable(nType.key()))
                            .withStyle(ChatFormatting.BLUE));
            adder.accept(
                    Component.translatable(
                                    "trait.rbmk.splitsInto", Component.translatable(rType.key()))
                            .withStyle(ChatFormatting.BLUE));
            adder.accept(
                    Component.translatable(
                                    "trait.rbmk.fluxFunc",
                                    ChatFormatting.WHITE + getFuncDescription(stack))
                            .withStyle(ChatFormatting.YELLOW));
            adder.accept(
                    Component.translatable(
                                    "trait.rbmk.funcType", Component.translatable(function.title()))
                            .withStyle(ChatFormatting.YELLOW));
            adder.accept(
                    Component.translatable(
                                    "trait.rbmk.xenonGen", ChatFormatting.WHITE + "x * " + xGen)
                            .withStyle(ChatFormatting.YELLOW));
            adder.accept(
                    Component.translatable(
                                    "trait.rbmk.xenonBurn", ChatFormatting.WHITE + "x² / " + xBurn)
                            .withStyle(ChatFormatting.YELLOW));
            adder.accept(
                    Component.translatable("trait.rbmk.heat", heat + "°C")
                            .withStyle(ChatFormatting.GOLD));
            adder.accept(
                    Component.translatable("trait.rbmk.diffusion", diffusion + "¹/²")
                            .withStyle(ChatFormatting.GOLD));
            adder.accept(
                    Component.translatable(
                                    "trait.rbmk.skinTemp",
                                    ((int) (getHullHeat(stack) * 10.0D) / 10.0D) + "°C")
                            .withStyle(ChatFormatting.RED));
            adder.accept(
                    Component.translatable(
                                    "trait.rbmk.coreTemp",
                                    ((int) (getCoreHeat(stack) * 10.0D) / 10.0D) + "°C")
                            .withStyle(ChatFormatting.RED));
            adder.accept(
                    Component.translatable("trait.rbmk.melt", meltingPoint + "°C")
                            .withStyle(ChatFormatting.DARK_RED));
        }
    }

    public interface BurnFunction {
        String title();

        double reactivity(RBMKRodItem rod, double flux, double enrichment);

        String formula(RBMKRodItem rod);
    }

    public interface DepletionFunction {
        double modify(double enrichment);
    }

    public enum EnumBurnFunc implements BurnFunction {
        PASSIVE("trait.rbmx.flux.passive"),
        LOG_TEN("trait.rbmx.flux.logten"),
        PLATEU("trait.rbmx.flux.euler"),
        ARCH("trait.rbmx.flux.arch"),
        SIGMOID("trait.rbmx.flux.sigmoid"),
        SQUARE_ROOT("trait.rbmx.flux.squrt"),
        LINEAR("trait.rbmx.flux.linear"),
        QUADRATIC("trait.rbmx.flux.quadratic"),
        EXPERIMENTAL("trait.rbmx.flux.experimental");

        private final String title;

        EnumBurnFunc(String title) {
            this.title = title;
        }

        @Override
        public String title() {
            return title;
        }

        @Override
        public double reactivity(RBMKRodItem rod, double flux, double enrichment) {
            double reactivity = rod.reactivity;
            return switch (this) {
                case PASSIVE -> rod.selfRate * enrichment;
                case LOG_TEN -> Math.log10(flux + 1) * 0.5D * reactivity;
                case PLATEU -> (1 - Math.pow(Math.E, -flux / 25D)) * reactivity;
                case ARCH -> Math.max((flux - (flux * flux / 10000D)) / 100D * reactivity, 0D);
                case SIGMOID -> reactivity / (1 + Math.pow(Math.E, -(flux - 50D) / 10D));
                case SQUARE_ROOT -> Math.sqrt(flux) * reactivity / 10D;
                case LINEAR -> flux / 100D * reactivity;
                case QUADRATIC -> flux * flux / 10000D * reactivity;
                case EXPERIMENTAL -> flux * (Math.sin(flux) + 1) * reactivity;
            };
        }

        @Override
        public String formula(RBMKRodItem rod) {
            return switch (this) {
                case PASSIVE -> ChatFormatting.RED + "" + rod.selfRate;
                case LOG_TEN -> "log10(%1$s + 1) * 0.5 * %2$s";
                case PLATEU -> "(1 - e^(-%1$s / 25)) * %2$s";
                case ARCH -> "(%1$s - %1$s² / 10000) / 100 * %2$s [0;∞]";
                case SIGMOID -> "%2$s / (1 + e^(-(%1$s - 50) / 10))";
                case SQUARE_ROOT -> "sqrt(%1$s) * %2$s / 10";
                case LINEAR -> "%1$s / 100 * %2$s";
                case QUADRATIC -> "%1$s² / 10000 * %2$s";
                case EXPERIMENTAL -> "%1$s * (sin(%1$s) + 1) * %2$s";
            };
        }
    }

    public enum EnumDepleteFunc implements DepletionFunction {
        LINEAR,
        RAISING_SLOPE,
        BOOSTED_SLOPE,
        GENTLE_SLOPE,
        STATIC;

        @Override
        public double modify(double enrichment) {
            return switch (this) {
                case STATIC -> 1D;
                case BOOSTED_SLOPE ->
                        enrichment + Math.sin((enrichment - 1) * (enrichment - 1) * Math.PI);
                case RAISING_SLOPE -> enrichment + (Math.sin(enrichment * Math.PI) / 2D);
                case GENTLE_SLOPE -> enrichment + (Math.sin(enrichment * Math.PI) / 3D);
                case LINEAR -> enrichment;
            };
        }
    }
}
