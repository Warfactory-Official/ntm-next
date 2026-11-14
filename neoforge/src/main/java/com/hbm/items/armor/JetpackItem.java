/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidContainerItem;
import com.hbm.util.TooltipStyle;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class JetpackItem extends ArmorModItem implements FluidContainerItem {

    private final Supplier<? extends Fluid> fuel;
    private final int capacity;
    public final ArmorSuitEffects.Jetpack kind;

    public JetpackItem(
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
        for (Component line :
                TooltipStyle.lines(
                        Component.translatableWithFallback(getDescriptionId() + ".desc", ""))) {
            adder.accept(line);
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

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(
                Component.translatable(
                                "desc.item.armorMod.installed",
                                stack.getHoverName(),
                                Component.translatable(
                                        "item.hbm.jetpack.fuel",
                                        NTMFluidProperties.getDisplayName(fuel.get()),
                                        getFuel(stack),
                                        capacity))
                        .withStyle(ChatFormatting.RED));
    }

    public Fluid fuel() {
        return fuel.get();
    }

    public int capacity() {
        return capacity;
    }

    public int getFuel(ItemInstance stack) {
        FluidStackNTM held =
                stack.getOrDefault(ModDataComponents.FLUID_CONTENT.get(), EMPTY_CONTENT);
        return held.type() == fuel.get() ? (int) held.amount() : 0;
    }

    public void burn(ItemStack stack, int age, int rate) {
        if (rate <= 0 || age % rate != 0) return;
        int left = getFuel(stack);
        if (left > 0) setContent(stack, new FluidStackNTM(fuel.get(), left - 1));
    }
}
