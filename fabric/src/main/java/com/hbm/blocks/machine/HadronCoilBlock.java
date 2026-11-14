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

package com.hbm.blocks.machine;

import com.mojang.serialization.MapCodec;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

public class HadronCoilBlock extends Block {
    public static final MapCodec<HadronCoilBlock> CODEC = simpleCodec(HadronCoilBlock::new);

    private static final int FACTOR = 10;

    public HadronCoilBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    public static class CoilItem extends BlockItem {

        public CoilItem(Block block, Properties props) {
            super(block, props);
        }

        @Override
        public void appendHoverText(
                ItemStack stack,
                Item.TooltipContext context,
                TooltipDisplay display,
                Consumer<Component> adder,
                TooltipFlag flag) {
            adder.accept(
                    Component.translatable("info.coil")
                            .append(": " + String.format(Locale.ROOT, "%,d", FACTOR)));
        }
    }
}
