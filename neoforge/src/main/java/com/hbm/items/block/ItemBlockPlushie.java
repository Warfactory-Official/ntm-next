// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.block;

import com.hbm.blocks.generic.BlockPlushie.PlushieType;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

public class ItemBlockPlushie extends ItemBlockTrinket<PlushieType> {

    public ItemBlockPlushie(Block block, Properties props, PlushieType type) {
        super(block, props, type);
    }

    @Override
    public Component getName(ItemStack stack) {
        return type == PlushieType.NONE
                ? super.getName(stack)
                : Component.translatable(getDescriptionId(), type.label);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, display, adder, flag);
        String inscription = type.inscription;
        if (inscription != null) adder.accept(Component.literal(inscription));
    }
}
