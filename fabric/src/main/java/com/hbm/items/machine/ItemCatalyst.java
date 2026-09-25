// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ItemCatalyst extends Item {

    private final int color;

    public ItemCatalyst(int color, Properties props) {
        super(props);
        this.color = color;
    }

    public int getColor() {
        return color;
    }

    public static int getColor(ItemStack stack) {
        return stack.getItem() instanceof ItemCatalyst catalyst ? catalyst.getColor() : 0;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(Component.translatable("desc.item.amsCatalyst.0"));
        adder.accept(Component.translatable("desc.item.amsCatalyst.1"));
    }
}
