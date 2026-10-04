// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import com.hbm.util.TooltipStyle;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ItemPileRod extends Item {

    private static final String SHARED_DESC = "desc.item.pileRod";

    public ItemPileRod(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        for (Component line :
                TooltipStyle.lines(Component.translatableWithFallback(SHARED_DESC, "")))
            adder.accept(line);
        for (Component line :
                TooltipStyle.lines(
                        Component.translatableWithFallback(getDescriptionId() + ".desc", ""))) {
            adder.accept(line);
        }
    }
}
