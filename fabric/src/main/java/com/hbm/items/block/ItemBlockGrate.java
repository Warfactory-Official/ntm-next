// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.block;

import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

public class ItemBlockGrate extends BlockItem {

    private final boolean wide;

    public ItemBlockGrate(Block block, Properties properties, boolean wide) {
        super(block, properties);
        this.wide = wide;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, display, adder, flag);
        if (wide) {
            adder.accept(
                    Component.translatable(
                            "desc.hbm."
                                    + BuiltInRegistries.BLOCK.getKey(getBlock()).getPath()
                                    + ".line1"));
        }
    }
}
