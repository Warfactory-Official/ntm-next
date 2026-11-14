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

package com.hbm.blocks.generic;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class WritingBlock extends Block {

    private static final String[] MESSAGE = {
        "chat.blockWriting.youShouldNotHave",
        "chat.blockWriting.thisIsNotA",
        "chat.blockWriting.nothingOfValueIs",
        "chat.blockWriting.whatIsHereIs",
        "chat.blockWriting.weConsideredOurselvesA",
        "chat.blockWriting.thenWeSawThe",
        "chat.blockWriting.andWeWereAfraid",
        "chat.blockWriting.weBuiltGreatTombs",
        "chat.blockWriting.ifThisPlaceIs",
        "chat.blockWriting.leaveThisPlaceAnd"
    };

    public WritingBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            for (String line : MESSAGE) {
                player.sendSystemMessage(
                        Component.translatable(line).withStyle(ChatFormatting.RED));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
