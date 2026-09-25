// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import com.hbm.items.ModDataComponents;
import com.hbm.util.BobMathUtil;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class ItemPlateFuel extends Item {

    public final int lifeTime;
    public final FunctionEnum function;
    public final int reactivity;
    private final Supplier<ItemDepletedFuel> spent;

    public ItemPlateFuel(
            Properties properties,
            int lifeTime,
            FunctionEnum function,
            int reactivity,
            Supplier<ItemDepletedFuel> spent) {
        super(properties);
        this.lifeTime = lifeTime;
        this.function = function;
        this.reactivity = reactivity;
        this.spent = spent;
    }

    public ItemDepletedFuel spent() {
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
                                "desc.item.plateFuel.yieldOf", BobMathUtil.getShortNumber(lifeTime))
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
