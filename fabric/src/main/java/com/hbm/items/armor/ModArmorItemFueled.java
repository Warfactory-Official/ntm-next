// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.IFluidContainerItem;
import com.hbm.util.BobMathUtil;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.material.Fluid;

public class ModArmorItemFueled extends ModArmorItem implements IFluidContainerItem {

    private final Supplier<Fluid> fuel;
    private final int maxFuel;
    private final int fillRate;
    private final int consumption;

    public ModArmorItemFueled(
            Properties properties,
            Suit suit,
            Supplier<Fluid> fuel,
            int maxFuel,
            int fillRate,
            int consumption) {
        super(properties, suit);
        this.fuel = fuel;
        this.maxFuel = maxFuel;
        this.fillRate = fillRate;
        this.consumption = consumption;
    }

    public Fluid fuel() {
        return fuel.get();
    }

    public int maxFuel() {
        return maxFuel;
    }

    public int getFill(ItemStack stack) {
        FluidStackNTM content = stack.get(ModDataComponents.FLUID_CONTENT.get());
        if (content == null) return maxFuel;
        return (int) Math.min(content.amount(), maxFuel);
    }

    public void setFill(ItemStack stack, int fill) {
        stack.set(
                ModDataComponents.FLUID_CONTENT.get(),
                new FluidStackNTM(fuel.get(), Math.clamp(fill, 0, maxFuel), 0));
    }

    @Override
    public boolean isEnabled(ItemStack stack) {
        return getFill(stack) > 0;
    }

    @Override
    public boolean drainsOnWear() {
        return true;
    }

    @Override
    public void drainForWear(ItemStack stack, int wear) {
        setFill(stack, getFill(stack) - wear * consumption);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getFill(stack) < maxFuel;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(getFill(stack) * 13F / maxFuel);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb((float) getFill(stack) / maxFuel / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {

        adder.accept(
                Component.literal(
                                NTMFluidProperties.clientName(fuel.get())
                                        + ": "
                                        + BobMathUtil.getShortNumber(getFill(stack))
                                        + " / "
                                        + BobMathUtil.getShortNumber(maxFuel))
                        .withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack, context, display, adder, flag);
    }

    @Override
    public boolean machineFillable() {
        return true;
    }

    @Override
    public int capacity(ItemStack stack) {
        return maxFuel;
    }

    @Override
    public FluidStackNTM getContent(ItemStack stack) {
        int fill = getFill(stack);
        if (fill <= 0) return EMPTY_CONTENT;
        return new FluidStackNTM(fuel.get(), fill, 0);
    }

    @Override
    public void setContent(ItemStack stack, FluidStackNTM content) {
        setFill(stack, content == null ? 0 : (int) content.amount());
    }

    private boolean acceptsFluid(Fluid type) {
        return type == fuel.get();
    }

    @Override
    public int fill(ItemStack stack, Fluid type, int amount, int pressure) {
        if (!acceptsFluid(type)) return 0;
        int fill = getFill(stack);
        int toFill = Math.min(Math.min(amount, fillRate), maxFuel - fill);
        if (toFill <= 0) return 0;
        setFill(stack, fill + toFill);
        return toFill;
    }

    @Override
    public int drain(ItemStack stack, Fluid type, int amount, int pressure) {
        return 0;
    }
}
