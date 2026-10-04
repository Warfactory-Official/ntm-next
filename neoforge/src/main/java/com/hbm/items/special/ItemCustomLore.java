// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.main.Polaroid;
import com.hbm.util.TooltipStyle;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ItemCustomLore extends Item {

    public ItemCustomLore(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        String base = getDescriptionId() + ".desc";
        if (Polaroid.isBalefireDay()) {
            var special = TooltipStyle.lines(Component.translatableWithFallback(base + ".P11", ""));
            if (!special.isEmpty()) {
                for (Component line : special) adder.accept(line);
                return;
            }
        }
        for (Component line : TooltipStyle.lines(Component.translatableWithFallback(base, "")))
            adder.accept(line);
    }
}
