// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import com.hbm.items.ModDataComponents;
import com.hbm.items.special.ItemNuclearWaste;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ItemDepletedFuel extends ItemNuclearWaste {

    public ItemDepletedFuel(Properties props) {
        super(props);
    }

    public static boolean isHot(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.DECAY_HEAT.get(), false);
    }

    public ItemStack hot() {
        ItemStack stack = new ItemStack(this);
        stack.set(ModDataComponents.DECAY_HEAT.get(), true);
        return stack;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        if (isHot(stack)) {
            adder.accept(
                    Component.translatable("desc.item.wasteCooling")
                            .withStyle(ChatFormatting.GOLD));
        }
    }
}
