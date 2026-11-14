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

package com.hbm.items.block;

import com.hbm.blocks.generic.PlushieBlock.Plushie;
import com.hbm.blocks.generic.PlushieBlock.PlushieType;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

public class PlushieBlockItem extends TrinketBlockItem<Plushie> {

    public PlushieBlockItem(Block block, Properties props, Plushie type) {
        super(block, props, type);
    }

    @Override
    public Component getName(ItemStack stack) {
        return type == PlushieType.NONE
                ? super.getName(stack)
                : Component.translatable(getDescriptionId(), type.label());
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, display, adder, flag);
        String inscription = type.inscription();
        if (inscription != null) adder.accept(Component.literal(inscription));
    }
}
