// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ItemLens extends Item {

    public ItemLens(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        int max = stack.getMaxDamage();
        if (max <= 0) return;
        int left = max - stack.getDamageValue();
        adder.accept(
                Component.translatable(
                        "desc.item.lensDurability", left, max, (int) (left * 100L / max)));
    }
}
