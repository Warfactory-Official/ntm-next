// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.block;

import com.hbm.blocks.generic.BlockSpeedy;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

public class ItemBlockBlastInfo extends BlockItem {

    public ItemBlockBlastInfo(Block block, Properties props) {
        super(block, props);
    }

    static Component line(Block block) {
        return Component.translatable(
                        "desc.block.blastInfo.blastResistance", block.getExplosionResistance())
                .withStyle(ChatFormatting.GOLD);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {

        if (getBlock() instanceof BlockSpeedy speedy) adder.accept(speedy.tooltip());
        super.appendHoverText(stack, context, display, adder, flag);
        adder.accept(line(getBlock()));
    }
}
