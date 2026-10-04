// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items;

import com.hbm.client.ModifierKeys;
import com.hbm.util.KeyHints;
import com.hbm.util.TooltipStyle;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

public final class ItemDroneWaypoint extends BlockItem {
    public ItemDroneWaypoint(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        if (ModifierKeys.detailsHeld()) {
            adder.accept(
                    Component.translatable("desc.hbm.drone_waypoint.line1")
                            .withStyle(ChatFormatting.YELLOW));
            adder.accept(
                    Component.translatable("desc.hbm.drone_waypoint.line2")
                            .withStyle(ChatFormatting.YELLOW));
            adder.accept(
                    Component.translatable(
                                    "tooltip.hbm.drone_waypoint.link",
                                    KeyHints.key("key.use", "key.mouse.right"))
                            .withStyle(ChatFormatting.YELLOW));
            adder.accept(
                    Component.translatable(
                                    "tooltip.hbm.drone_waypoint.clear",
                                    KeyHints.key("key.use", "key.mouse.right"))
                            .withStyle(ChatFormatting.YELLOW));
            adder.accept(
                    Component.translatable(
                                    "tooltip.hbm.drone_waypoint.offset",
                                    KeyHints.key("key.use", "key.mouse.right"),
                                    KeyHints.key("key.sneak", "key.keyboard.left.shift"))
                            .withStyle(ChatFormatting.YELLOW));
        } else {
            adder.accept(TooltipStyle.holdDetails());
        }
        super.appendHoverText(stack, context, display, adder, flag);
    }
}
