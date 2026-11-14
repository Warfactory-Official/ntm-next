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

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

public final class DiamondGravelBlockItem extends BlockItem {

    public DiamondGravelBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(Component.translatable("desc.item.diamondGravel.thereIsSomeKind"));
        adder.accept(Component.translatable("desc.item.diamondGravel.butICanT"));
        adder.accept(Component.empty());
        adder.accept(Component.translatable("desc.item.diamondGravel.update20200704"));
        adder.accept(Component.translatable("desc.item.diamondGravel.weDenyAnyImplications"));
        adder.accept(Component.translatable("desc.item.diamondGravel.theBasisThatIt"));
        adder.accept(Component.translatable("desc.item.diamondGravel.thatPeopleStartedStabbing"));
        adder.accept(Component.empty());
        adder.accept(Component.translatable("desc.item.diamondGravel.update20201704"));
        adder.accept(Component.translatable("desc.item.diamondGravel.asItTurnsOut"));
        adder.accept(Component.translatable("desc.item.diamondGravel.neverReallyAThing"));
        adder.accept(Component.translatable("desc.item.diamondGravel.haveBeenAJoke"));
        adder.accept(Component.translatable("desc.item.diamondGravel.weApologizeForGetting"));
        adder.accept(Component.translatable("desc.item.diamondGravel.thisNonJokeThat"));
        adder.accept(Component.empty());
        adder.accept(Component.translatable("desc.item.diamondGravel.iAddedAnItem"));
    }
}
