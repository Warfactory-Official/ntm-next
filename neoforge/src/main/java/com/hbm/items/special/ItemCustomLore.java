// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.main.Polaroid;
import com.hbm.util.I18nUtil;
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
        String base = this.getDescriptionId() + ".desc";

        if (Polaroid.isBalefireDay()) {
            String[] special = I18nUtil.loreLines(base + ".P11");
            if (special.length > 0) {
                for (String line : special) adder.accept(Component.literal(line));
                return;
            }
        }
        for (String line : I18nUtil.loreLines(base)) adder.accept(Component.literal(line));
    }
}
