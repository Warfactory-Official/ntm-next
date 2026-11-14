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
import com.hbm.util.TooltipStyle;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

public class DescBlockItem extends BlockItem {

    public enum Desc {
        STANDARD,
        YELLOW,
        PLAIN
    }

    private final int descLines;
    private final Desc mode;

    public DescBlockItem(Block block, Properties props, int descLines) {
        this(block, props, descLines, Desc.STANDARD);
    }

    public DescBlockItem(Block block, Properties props, int descLines, Desc mode) {
        super(block, props);
        this.descLines = descLines;
        this.mode = mode;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        if (descLines > 0) {
            if (mode != Desc.STANDARD || ModifierKeys.detailsHeld()) {
                Identifier id = BuiltInRegistries.BLOCK.getKey(getBlock());
                String key = "desc." + id.getNamespace() + "." + id.getPath();
                for (int i = 1; i <= descLines; i++) {
                    MutableComponent line = Component.translatable(key + ".line" + i);
                    adder.accept(mode == Desc.PLAIN ? line : line.withStyle(ChatFormatting.YELLOW));
                }
            } else {
                adder.accept(TooltipStyle.holdDetails());
            }
        }
        super.appendHoverText(stack, context, display, adder, flag);
    }
}
