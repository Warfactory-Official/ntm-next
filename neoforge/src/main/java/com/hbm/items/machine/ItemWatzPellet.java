// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.util.function.Function.FunctionLinear;
import com.hbm.util.function.Function.FunctionQuadratic;
import com.hbm.util.function.Function.FunctionSqrt;
import com.hbm.util.function.Function.FunctionSqrtFalling;
import com.hbm.util.function.Function;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

public class ItemWatzPellet extends Item {
    public static final int SOURCE_LIGHT = 0xD2D2D2;
    public static final int SOURCE_DARK = 0x333333;
    public final boolean depleted;
    public final WatzFuel type;

    public ItemWatzPellet(Properties properties, boolean depleted, WatzFuel type) {
        super(properties);
        this.depleted = depleted;
        this.type = type;
    }

    public static String spriteName(String item, WatzFuel type) {
        return item + "." + type.name().toLowerCase(Locale.US);
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
        return ((ItemWatzPellet) stack.getItem()).type;
    }

    public static boolean isFresh(ItemStack stack) {
        return stack.getItem() instanceof ItemWatzPellet pellet && !pellet.depleted;
    }

    public static boolean isDepleted(ItemStack stack) {
        return stack.getItem() instanceof ItemWatzPellet pellet && pellet.depleted;
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
                                        Locale.US, "%.1f", (1D - getEnrichment(stack)) * 100D))
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
        Function burnFunc = num.burnFunc();
        if (burnFunc != null) {
            adder.accept(
                    Component.translatable(
                            "desc.shared.reactionFunction", burnFunc.getLabelForFuel()));
            adder.accept(
                    Component.translatable("desc.shared.fuelType", burnFunc.getDangerFromFuel()));
        }
        Function heatDiv = num.heatDiv();
        if (heatDiv != null) {
            adder.accept(
                    Component.translatable(
                            "desc.item.watzPellet.thermalMultiplier", heatDiv.getLabelForFuel()));
        }
        Function absorbFunc = num.absorbFunc();
        if (absorbFunc != null) {
            adder.accept(
                    Component.translatable(
                            "desc.item.watzPellet.fluxCapture", absorbFunc.getLabelForFuel()));
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

        @Nullable Function burnFunc();

        @Nullable Function heatDiv();

        @Nullable Function absorbFunc();

        ItemStack depletedStack();
    }

    public enum EnumWatzType implements WatzFuel {
        SCHRABIDIUM(
                0x32FFFF,
                0x005C5C,
                2_000,
                20D,
                0.01D,
                new FunctionLinear(1.5D),
                new FunctionSqrtFalling(10D),
                null),
        HES(
                0x66DCD6,
                0x023933,
                1_750,
                20D,
                0.005D,
                new FunctionLinear(1.25D),
                new FunctionSqrtFalling(15D),
                null),
        MES(
                0xCBEADF,
                0x28473C,
                1_500,
                15D,
                0.0025D,
                new FunctionLinear(1.15D),
                new FunctionSqrtFalling(15D),
                null),
        LES(
                0xABB4A8,
                0x0C1105,
                1_250,
                15D,
                0.00125D,
                new FunctionLinear(1D),
                new FunctionSqrtFalling(20D),
                null),
        HEN(
                0xA6B2A6,
                0x030F03,
                0,
                10D,
                0.0005D,
                new FunctionSqrt(100),
                new FunctionSqrtFalling(10D),
                null),
        MEU(
                0xC1C7BD,
                0x2B3227,
                0,
                10D,
                0.0005D,
                new FunctionSqrt(75),
                new FunctionSqrtFalling(10D),
                null),
        MEP(
                0x9AA3A0,
                0x111A17,
                0,
                15D,
                0.0005D,
                new FunctionSqrt(150),
                new FunctionSqrtFalling(10D),
                null),
        LEAD(0xA6A6B2, 0x03030F, 0, 0, 0.0025D, null, null, new FunctionSqrt(10)),
        BORON(0xBDC8D2, 0x29343E, 0, 0, 0.0025D, null, null, new FunctionLinear(10)),
        DU(
                0xC1C7BD,
                0x2B3227,
                0,
                0,
                0.0025D,
                null,
                null,
                new FunctionQuadratic(1D, 1D).withDiv(100)),
        NQD(
                0x4B4B4B,
                0x121212,
                2_000,
                20,
                0.01D,
                new FunctionLinear(2D),
                new FunctionSqrt(1D / 25D).withOff(25D * 25D),
                null),
        NQR(
                0x2D2D2D,
                0x0B0B0B,
                2_500,
                30,
                0.01D,
                new FunctionLinear(1.5D),
                new FunctionSqrt(1D / 25D).withOff(25D * 25D),
                null);

        public final double yield = 500_000_000;
        public final int colorLight;
        public final int colorDark;
        public final double mudContent;
        public final double passive;
        public final double heatEmission;
        public final @Nullable Function burnFunc;
        public final @Nullable Function heatDiv;
        public final @Nullable Function absorbFunc;

        EnumWatzType(
                int colorLight,
                int colorDark,
                double passive,
                double heatEmission,
                double mudContent,
                @Nullable Function burnFunction,
                @Nullable Function heatDivisor,
                @Nullable Function absorbFunction) {
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
        public @Nullable Function burnFunc() {
            return burnFunc;
        }

        @Override
        public @Nullable Function heatDiv() {
            return heatDiv;
        }

        @Override
        public @Nullable Function absorbFunc() {
            return absorbFunc;
        }

        @Override
        public ItemStack depletedStack() {
            return ModItems.WATZ_PELLET_DEPLETED.stack(this);
        }
    }
}
