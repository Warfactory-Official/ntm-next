// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import com.hbm.blockentity.machine.rbmk.IRBMKFluxReceiver.NType;
import com.hbm.blockentity.machine.rbmk.RBMKConfig;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
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

public class ItemRBMKRod extends Item {
    public @Nullable ItemRBMKPellet pellet;

    public String fullName = "";
    public double reactivity;
    public double selfRate;
    public EnumBurnFunc function = EnumBurnFunc.LOG_TEN;
    public EnumDepleteFunc depFunc = EnumDepleteFunc.GENTLE_SLOPE;
    public double xGen = 0.5D;
    public double xBurn = 50.0D;
    public double heat = 1.0D;
    public double yield;
    public double meltingPoint = 1000.0D;
    public double diffusion = 0.02D;
    public NType nType = NType.SLOW;
    public NType rType = NType.FAST;
    public int colorTint = 0x304825;
    public double heatCoeffStart = 0.0D;
    public double heatCoeffLength = 0.0D;

    public ItemRBMKRod(Properties props, String fullName) {
        super(props);
        this.fullName = fullName;
    }

    public static double getEnrichment(ItemStack stack) {
        return getYield(stack) / ((ItemRBMKRod) stack.getItem()).yield;
    }

    public static double getPoisonLevel(ItemStack stack) {
        return getPoison(stack) / 100.0D;
    }

    public static RBMKFuelData getData(ItemStack stack) {
        RBMKFuelData d = stack.get(ModDataComponents.RBMK_FUEL.get());
        if (d != null) return d;
        return RBMKFuelData.fresh(stack.getItem() instanceof ItemRBMKRod rod ? rod.yield : 0);
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

    public ItemRBMKRod setTint(int tint) {
        colorTint = tint;
        return this;
    }

    public ItemRBMKRod setYield(double yield) {
        this.yield = yield;
        return this;
    }

    public ItemRBMKRod setStats(double funcEnd) {
        return setStats(funcEnd, 0);
    }

    public ItemRBMKRod setStats(double funcEnd, double selfRate) {
        reactivity = funcEnd;
        this.selfRate = selfRate;
        return this;
    }

    public ItemRBMKRod setFunction(EnumBurnFunc func) {
        function = func;
        return this;
    }

    public ItemRBMKRod setDepletionFunction(EnumDepleteFunc func) {
        depFunc = func;
        return this;
    }

    public ItemRBMKRod setHeatCoeff(double start, double length) {
        heatCoeffStart = start;
        heatCoeffLength = length;
        return this;
    }

    public ItemRBMKRod setXenon(double gen, double burn) {
        xGen = gen;
        xBurn = burn;
        return this;
    }

    public ItemRBMKRod setHeat(double heat) {
        this.heat = heat;
        return this;
    }

    public ItemRBMKRod setDiffusion(double diffusion) {
        this.diffusion = diffusion;
        return this;
    }

    public ItemRBMKRod setMeltingPoint(double meltingPoint) {
        this.meltingPoint = meltingPoint;
        return this;
    }

    public ItemRBMKRod setNeutronTypes(NType nType, NType rType) {
        this.nType = nType;
        this.rType = rType;
        return this;
    }

    public double burn(Level world, ItemStack stack, double inFlux) {

        inFlux += selfRate;

        if (RBMKConfig.getXenon(world)) {
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
                        * RBMKConfig.getReactivityMod(world);

        if (RBMKConfig.getDepletion(world)) {
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
            coreHeat -= mid * diffusion * RBMKConfig.getFuelDiffusionMod(world) * mod;
            hullHeat += mid * diffusion * RBMKConfig.getFuelDiffusionMod(world) * mod;
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
        ret *= RBMKConfig.getFuelHeatProvision(world) * mod;
        hullHeat -= ret;
        setHullHeat(stack, hullHeat);
        return ret;
    }

    public double reactivityFunc(double in, double enrichment) {
        double flux = in * reactivityModByEnrichment(enrichment);
        return switch (function) {
            case PASSIVE -> selfRate * enrichment;
            case LOG_TEN -> Math.log10(flux + 1) * 0.5D * reactivity;
            case PLATEU -> (1 - Math.pow(Math.E, -flux / 25.0D)) * reactivity;
            case ARCH -> Math.max((flux - (flux * flux / 10000.0D)) / 100.0D * reactivity, 0.0D);
            case SIGMOID -> reactivity / (1 + Math.pow(Math.E, -(flux - 50.0D) / 10.0D));
            case SQUARE_ROOT -> Math.sqrt(flux) * reactivity / 10.0D;
            case LINEAR -> flux / 100.0D * reactivity;
            case QUADRATIC -> flux * flux / 10000.0D * reactivity;
            case EXPERIMENTAL -> flux * (Math.sin(flux) + 1) * reactivity;
        };
    }

    public double reactivityModByEnrichment(double enrichment) {
        return switch (depFunc) {
            case STATIC -> 1.0D;
            case BOOSTED_SLOPE ->
                    enrichment + Math.sin((enrichment - 1) * (enrichment - 1) * Math.PI);
            case RAISING_SLOPE -> enrichment + (Math.sin(enrichment * Math.PI) / 2.0D);
            case GENTLE_SLOPE -> enrichment + (Math.sin(enrichment * Math.PI) / 3.0D);
            default -> enrichment;
        };
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
        String function =
                switch (this.function) {
                    case PASSIVE -> ChatFormatting.RED + "" + selfRate;
                    case LOG_TEN -> "log10(%1$s + 1) * 0.5 * %2$s";
                    case PLATEU -> "(1 - e^(-%1$s / 25)) * %2$s";
                    case ARCH -> "(%1$s - %1$s² / 10000) / 100 * %2$s [0;∞]";
                    case SIGMOID -> "%2$s / (1 + e^(-(%1$s - 50) / 10))";
                    case SQUARE_ROOT -> "sqrt(%1$s) * %2$s / 10";
                    case LINEAR -> "%1$s / 100 * %2$s";
                    case QUADRATIC -> "%1$s² / 10000 * %2$s";
                    case EXPERIMENTAL -> "%1$s * (sin(%1$s) + 1) * %2$s";
                };

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
            return String.format(Locale.US, function, flux, reactivity).concat(enrichmentPer);
        }

        return String.format(Locale.US, function, flux, reactivity);
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
                                    Component.translatable(nType.unlocalized + ".x"))
                            .withStyle(ChatFormatting.BLUE));
            adder.accept(
                    Component.translatable(
                                    "trait.rbmx.splitsInto",
                                    Component.translatable(rType.unlocalized + ".x"))
                            .withStyle(ChatFormatting.BLUE));
            adder.accept(
                    Component.translatable(
                                    "trait.rbmx.fluxFunc",
                                    ChatFormatting.WHITE + getFuncDescription(stack))
                            .withStyle(ChatFormatting.YELLOW));
            adder.accept(
                    Component.translatable(
                                    "trait.rbmx.funcType", Component.translatable(function.title))
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
                                    "trait.rbmk.splitsWith",
                                    Component.translatable(nType.unlocalized))
                            .withStyle(ChatFormatting.BLUE));
            adder.accept(
                    Component.translatable(
                                    "trait.rbmk.splitsInto",
                                    Component.translatable(rType.unlocalized))
                            .withStyle(ChatFormatting.BLUE));
            adder.accept(
                    Component.translatable(
                                    "trait.rbmk.fluxFunc",
                                    ChatFormatting.WHITE + getFuncDescription(stack))
                            .withStyle(ChatFormatting.YELLOW));
            adder.accept(
                    Component.translatable(
                                    "trait.rbmk.funcType", Component.translatable(function.title))
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

    public enum EnumBurnFunc {
        PASSIVE("trait.rbmx.flux.passive"),
        LOG_TEN("trait.rbmx.flux.logten"),
        PLATEU("trait.rbmx.flux.euler"),
        ARCH("trait.rbmx.flux.arch"),
        SIGMOID("trait.rbmx.flux.sigmoid"),
        SQUARE_ROOT("trait.rbmx.flux.squrt"),
        LINEAR("trait.rbmx.flux.linear"),
        QUADRATIC("trait.rbmx.flux.quadratic"),
        EXPERIMENTAL("trait.rbmx.flux.experimental");

        public final String title;

        EnumBurnFunc(String title) {
            this.title = title;
        }
    }

    public enum EnumDepleteFunc {
        LINEAR,
        RAISING_SLOPE,
        BOOSTED_SLOPE,
        GENTLE_SLOPE,
        STATIC
    }
}
