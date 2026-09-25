// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import com.hbm.items.machine.ItemPWRFuel.EnumPWRFuel;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ItemPWRFuelStage extends Item {

    private final boolean hot;
    public final EnumPWRFuel fuel;

    public ItemPWRFuelStage(Properties props, boolean hot, EnumPWRFuel fuel) {
        super(props);
        this.hot = hot;
        this.fuel = fuel;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {

        adder.accept(
                Component.translatable("item.hbm.pwr_fuel_" + fuel.id)
                        .withStyle(ChatFormatting.ITALIC));
        if (hot)
            adder.accept(
                    Component.translatable("desc.item.wasteCooling")
                            .withStyle(ChatFormatting.GOLD));
    }
}
