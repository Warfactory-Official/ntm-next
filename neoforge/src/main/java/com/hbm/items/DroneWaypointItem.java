/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

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

public final class DroneWaypointItem extends BlockItem {
    public DroneWaypointItem(Block block, Properties properties) {
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
