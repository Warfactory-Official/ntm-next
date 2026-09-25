// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.IFluidContainerItem;
import com.hbm.util.I18nUtil;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class ItemJetpack extends ItemArmorMod implements IFluidContainerItem {

    private final Supplier<? extends Fluid> fuel;
    private final int capacity;
    public final ArmorSuitEffects.Jetpack kind;

    public ItemJetpack(
            Properties properties,
            Supplier<? extends Fluid> fuel,
            int capacity,
            ArmorSuitEffects.Jetpack kind) {
        super(properties, ArmorModHandler.PLATE_ONLY, false, true, false, false);
        this.fuel = fuel;
        this.capacity = capacity;
        this.kind = kind;
    }

    @Override
    public boolean canStore(Fluid type) {
        return false;
    }

    @Override
    public boolean machineFillable() {
        return true;
    }

    @Override
    public int capacity(ItemStack stack) {
        return capacity;
    }

    @Override
    public FluidStackNTM getContent(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.FLUID_CONTENT.get(), EMPTY_CONTENT);
    }

    @Override
    public void setContent(ItemStack stack, FluidStackNTM content) {
        if (content == null || content.type() == Fluids.EMPTY || content.amount() <= 0) {
            stack.remove(ModDataComponents.FLUID_CONTENT.get());
            return;
        }
        stack.set(
                ModDataComponents.FLUID_CONTENT.get(),
                new FluidStackNTM(
                        content.type(), Math.min(content.amount(), capacity), content.pressure()));
    }

    @Override
    public int fill(ItemStack stack, Fluid type, int amount, int pressure) {
        if (type != fuel.get() || amount <= 0) return 0;
        FluidStackNTM held = getContent(stack);
        if (held.type() != Fluids.EMPTY && held.pressure() != pressure) return 0;
        int have = held.type() == Fluids.EMPTY ? 0 : (int) held.amount();
        int accepted = Math.min(amount, capacity - have);
        if (accepted <= 0) return 0;
        setContent(stack, new FluidStackNTM(type, have + accepted, pressure));
        return accepted;
    }

    @Override
    public int drain(ItemStack stack, Fluid type, int amount, int pressure) {
        return 0;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        for (String line : I18nUtil.loreLines(getDescriptionId() + ".desc")) {
            adder.accept(Component.literal(line));
        }
        adder.accept(
                Component.translatable(
                                "item.hbm.jetpack.fuel",
                                NTMFluidProperties.getDisplayName(fuel.get()),
                                getFuel(stack),
                                capacity)
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
        adder.accept(Component.empty());
        adder.accept(
                Component.translatable("item.hbm.jetpack.wearable").withStyle(ChatFormatting.GOLD));
    }

    public int getFuel(ItemStack stack) {
        FluidStackNTM held = getContent(stack);
        return held.type() == fuel.get() ? (int) held.amount() : 0;
    }

    public void burn(ItemStack stack, int age, int rate) {
        if (rate <= 0 || age % rate != 0) return;
        int left = getFuel(stack);
        if (left > 0) setContent(stack, new FluidStackNTM(fuel.get(), left - 1));
    }
}
